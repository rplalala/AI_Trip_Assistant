package com.demo.api.service.impl;

import com.demo.api.client.BookingApiClient;
import com.demo.api.dto.booking.*;
import com.demo.api.exception.BookingApiException;
import com.demo.api.model.*;
import com.demo.api.repository.*;
import com.demo.api.service.BookingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for handling booking logic via external Booking API.
 * Supports quoting individual items and bundled itineraries.
 */
@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final String STATUS_CONFIRMED = "confirm";
    private static final String STATUS_FAILED = "failed";
    private static final String DEFAULT_CURRENCY = "AUD";

    private final BookingApiClient bookingApiClient;
    private final TripTransportationRepository tripTransportationRepository;
    private final TripHotelRepository tripHotelRepository;
    private final TripAttractionRepository tripAttractionRepository;
    private final TripBookingQuoteRepository tripBookingQuoteRepository;
    private final TripPreferenceRepository tripPreferenceRepository;
    private final ObjectMapper objectMapper;

    public BookingServiceImpl(BookingApiClient bookingApiClient,
                              TripTransportationRepository tripTransportationRepository,
                              TripHotelRepository tripHotelRepository,
                              TripAttractionRepository tripAttractionRepository,
                              TripBookingQuoteRepository tripBookingQuoteRepository,
                              TripPreferenceRepository tripPreferenceRepository,
                              ObjectMapper objectMapper) {
        this.bookingApiClient = bookingApiClient;
        this.tripTransportationRepository = tripTransportationRepository;
        this.tripHotelRepository = tripHotelRepository;
        this.tripAttractionRepository = tripAttractionRepository;
        this.tripBookingQuoteRepository = tripBookingQuoteRepository;
        this.tripPreferenceRepository = tripPreferenceRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles quoting a single reservation-required trip item (e.g., one hotel/flight).
     * Sends the appropriate QuoteReq payload to the external Booking API, then
     * stores the response as a TripBookingQuote record.
     */
    @Override
    public TripBookingQuote quoteSingleItem(Long tripId, String productType, Long entityId) {
        Assert.notNull(tripId, "tripId must not be null");
        Assert.notNull(entityId, "entityId must not be null");
        TripPreference preference = fetchPreference(tripId);
        String normalizedType = normalizeProductType(productType);
        log.debug("Preparing single quote for tripId={}, productType={}, entityId={}", tripId, normalizedType, entityId);

        QuotePayload payload = buildQuotePayload(normalizedType, entityId, preference, true);
        QuoteReq request = new QuoteReq(
                normalizedType,
                payload.currency(),
                resolvePartySize(preference),
                payload.params(),
                tripId,
                entityId,
                payload.reference()
        );

        try {
            QuoteResp response = bookingApiClient.postQuote(request);
            TripBookingQuote saved = upsertQuoteRecord(
                    tripId,
                    normalizedType,
                    entityId,
                    response.voucherCode(),
                    response.invoiceId(),
                    payload.currency(),
                    computeTotalAmount(response.items()),
                    serializeSafely(response),
                    STATUS_CONFIRMED
            );
            markEntityConfirmed(saved);
            log.info("Stored booking quote for tripId={}, reference={}, voucher={}",
                    tripId, saved.getItemReference(), response.voucherCode());
            return saved;
        } catch (BookingApiException ex) {
            persistFailure(tripId, normalizedType, entityId, ex.getResponseBody() != null ? ex.getResponseBody() : ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            persistFailure(tripId, normalizedType, entityId, ex.getMessage());
            throw ex;
        }
    }


    /**
     * Handles quoting all pending items in a trip (e.g., hotel + transport + attraction).
     * Builds a bundled ItineraryQuoteReq, sends it to Booking API,
     * then stores each item response in TripBookingQuote table.
     */
    @Override
    public ItineraryQuoteResp quoteItinerary(Long tripId) {
        Assert.notNull(tripId, "tripId must not be null");
        TripPreference preference = fetchPreference(tripId);

        List<ItineraryItemContext> contexts = collectPendingItems(tripId, preference);
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("No pending reservation-required items for trip " + tripId);
        }

        String requestCurrency = preference.getCurrency() != null ? preference.getCurrency() : DEFAULT_CURRENCY;
        List<ItineraryQuoteReqItem> requestItems = contexts.stream()
                .map(ctx -> new ItineraryQuoteReqItem(
                        ctx.reference(),
                        ctx.productType(),
                        resolvePartySize(preference),
                        ctx.params(),
                        ctx.entityId()
                ))
                .collect(Collectors.toList());

        ItineraryQuoteReq request = new ItineraryQuoteReq("iti_" + tripId, requestCurrency, requestItems, tripId);

        try {
            ItineraryQuoteResp response = bookingApiClient.postItineraryQuote(request);
            String rawResponse = serializeSafely(response);
            for (ItineraryQuoteItem item : response.items()) {
                String itemReference = item.reference();
                ItineraryItemContext ctx = contexts.stream()
                        .filter(candidate -> candidate.reference().equals(itemReference))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unexpected itinerary item reference: " + itemReference));
                String itemCurrency = resolveItemCurrency(item, ctx, response.currency());

                TripBookingQuote saved = upsertQuoteRecord(
                        tripId,
                        ctx.productType(),
                        ctx.entityId(),
                        response.voucherCode(),
                        response.invoiceId(),
                        itemCurrency,
                        toIntegerAmount(item.total()),
                        rawResponse,
                        STATUS_CONFIRMED
                );
                markEntityConfirmed(saved);
            }
            log.info("Stored itinerary quote for tripId={}, voucher={}", tripId, response.voucherCode());
            return response;
        } catch (BookingApiException ex) {
            String errorPayload = ex.getResponseBody() != null ? ex.getResponseBody() : ex.getMessage();
            contexts.forEach(ctx -> persistFailure(tripId, ctx.productType(), ctx.entityId(), errorPayload));
            throw ex;
        } catch (RuntimeException ex) {
            contexts.forEach(ctx -> persistFailure(tripId, ctx.productType(), ctx.entityId(), ex.getMessage()));
            throw ex;
        }
    }

    private TripPreference fetchPreference(Long tripId) {
        return tripPreferenceRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip preference not found for tripId " + tripId));
    }

    /**
     * Builds QuotePayload for a given productType (transportation/hotel/attraction),
     * including fallback logic from TripPreference when required.
     */
    private QuotePayload buildQuotePayload(String productType, Long entityId, TripPreference preference, boolean validateTrip) {
        return switch (productType) {
            case "transportation" -> buildTransportationPayload(entityId, preference, validateTrip);
            case "hotel" -> buildHotelPayload(entityId, preference, validateTrip);
            case "attraction" -> buildAttractionPayload(entityId, preference, validateTrip);
            default -> throw new IllegalArgumentException("Unsupported product type: " + productType);
        };
    }

    /**
     * Builds the 'params' map for transportation quote based on TripTransportation entity.
     * Includes fields like mode, from, to, class, date, provider.
     */
    private QuotePayload buildTransportationPayload(Long entityId, TripPreference preference, boolean validateTrip) {
        TripTransportation entity = tripTransportationRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Transportation not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getId()),
                    "Transportation does not belong to trip " + preference.getId());
        }
        LocalDate date = Objects.requireNonNull(entity.getDate(), "Transportation date is required");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("mode", inferTransportationMode(entity.getProvider()));
        params.put("from", entity.getFrom());
        params.put("to", entity.getTo());
        params.put("date", date.toString());
        params.put("class", StringUtils.hasText(entity.getTicketType()) ? entity.getTicketType() : "economy");
        if (StringUtils.hasText(entity.getProvider())) {
            params.put("provider", entity.getProvider());
        }
        if (entity.getPrice() != null) {
            params.put("price", entity.getPrice());
        }
        return new QuotePayload(
                "transportation",
                entity.getId(),
                params,
                resolveCurrency(entity.getCurrency(), preference),
                buildReference("transportation", entity.getId())
        );
    }

    /**
     * Builds the 'params' map for hotel quote based on TripHotel entity.
     * Includes fields like hotelName, checkIn, checkOut, roomType, stars.
     */
    private QuotePayload buildHotelPayload(Long entityId, TripPreference preference, boolean validateTrip) {
        TripHotel entity = tripHotelRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getId()),
                    "Hotel does not belong to trip " + preference.getId());
        }
        LocalDate checkIn = Objects.requireNonNull(entity.getDate(), "Hotel check-in date is required");
        int nights = entity.getNights() != null && entity.getNights() > 0 ? entity.getNights() : 1;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("city", defaultString(preference.getToCity(), "Unknown city"));
        params.put("checkIn", checkIn.toString());
        params.put("checkOut", checkIn.plusDays(nights).toString());
        params.put("roomType", entity.getRoomType());
        params.put("stars", 3);
        if (StringUtils.hasText(entity.getHotelName())) {
            params.put("hotelName", entity.getHotelName());
        }
        params.put("nights", nights);
        if (entity.getPeople() != null) {
            params.put("people", entity.getPeople());
        }
        if (entity.getPrice() != null) {
            params.put("price", entity.getPrice());
        }
        return new QuotePayload(
                "hotel",
                entity.getId(),
                params,
                resolveCurrency(entity.getCurrency(), preference),
                buildReference("hotel", entity.getId())
        );
    }

    /**
     * Builds the 'params' map for attraction quote based on TripAttraction entity.
     * Includes fields like name, city, date, session, ticketPrice.
     */
    private QuotePayload buildAttractionPayload(Long entityId, TripPreference preference, boolean validateTrip) {
        TripAttraction entity = tripAttractionRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Attraction not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getId()),
                    "Attraction does not belong to trip " + preference.getId());
        }
        LocalDate date = Objects.requireNonNull(entity.getDate(), "Attraction date is required");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("city", StringUtils.hasText(entity.getLocation()) ? entity.getLocation()
                : defaultString(preference.getToCity(), "Unknown city"));
        params.put("name", entity.getTitle());
        params.put("date", date.toString());
        params.put("session", entity.getTime());
        if (entity.getTicketPrice() != null) {
            params.put("ticketPrice", entity.getTicketPrice());
        }
        if (entity.getPeople() != null) {
            params.put("people", entity.getPeople());
        }
        if (entity.getTicketPrice() != null && entity.getPeople() != null) {
            params.put("price", entity.getTicketPrice() * Math.max(1, entity.getPeople()));
        } else if (entity.getTicketPrice() != null) {
            params.put("price", entity.getTicketPrice());
        }
        return new QuotePayload(
                "attraction",
                entity.getId(),
                params,
                resolveCurrency(entity.getCurrency(), preference),
                buildReference("attraction", entity.getId())
        );
    }

    /**
     * Collects all reservation-required items that are still pending confirmation.
     * Builds a context list used to generate itinerary quotes.
     */
    private List<ItineraryItemContext> collectPendingItems(Long tripId, TripPreference preference) {
        List<ItineraryItemContext> contexts = new ArrayList<>();
        tripTransportationRepository.findByTripId(tripId).stream()
                .filter(item -> shouldQuote(item.getReservationRequired(), item.getStatus()))
                .map(item -> {
                    QuotePayload payload = buildTransportationPayload(item.getId(), preference, false);
                    return new ItineraryItemContext(payload.productType(), payload.entityId(),
                            buildReference(payload.productType(), payload.entityId()), payload.params(), payload.currency());
                })
                .forEach(contexts::add);

        tripHotelRepository.findByTripId(tripId).stream()
                .filter(item -> shouldQuote(item.getReservationRequired(), item.getStatus()))
                .map(item -> {
                    QuotePayload payload = buildHotelPayload(item.getId(), preference, false);
                    return new ItineraryItemContext(payload.productType(), payload.entityId(),
                            buildReference(payload.productType(), payload.entityId()), payload.params(), payload.currency());
                })
                .forEach(contexts::add);

        tripAttractionRepository.findByTripId(tripId).stream()
                .filter(item -> shouldQuote(item.getReservationRequired(), item.getStatus()))
                .map(item -> {
                    QuotePayload payload = buildAttractionPayload(item.getId(), preference, false);
                    return new ItineraryItemContext(payload.productType(), payload.entityId(),
                            buildReference(payload.productType(), payload.entityId()), payload.params(), payload.currency());
                })
                .forEach(contexts::add);

        return contexts;
    }

    private TripBookingQuote upsertQuoteRecord(Long tripId,
                                               String productType,
                                               Long entityId,
                                               String voucherCode,
                                               String invoiceId,
                                               String currency,
                                               Integer totalAmount,
                                               String rawResponse,
                                               String status) {
        TripBookingQuote quote = tripBookingQuoteRepository.findByTripIdAndEntityIdAndProductType(tripId, entityId, productType)
                .orElseGet(() -> TripBookingQuote.builder()
                        .tripId(tripId)
                        .productType(productType)
                        .entityId(entityId)
                        .itemReference(buildReference(productType, entityId))
                        .build());

        quote.setProductType(productType);
        quote.setItemReference(buildReference(productType, entityId));
        quote.setVoucherCode(voucherCode);
        quote.setInvoiceId(invoiceId);
        quote.setCurrency(currency);
        quote.setTotalAmount(totalAmount);
        quote.setRawResponse(rawResponse);
        quote.setStatus(status);
        return tripBookingQuoteRepository.save(quote);
    }

    private void markEntityConfirmed(TripBookingQuote quote) {
        switch (quote.getProductType()) {
            case "transportation", "transport" -> tripTransportationRepository.findById(quote.getEntityId())
                    .ifPresent(entity -> {
                        entity.setStatus(STATUS_CONFIRMED);
                        tripTransportationRepository.save(entity);
                    });
            case "hotel" -> tripHotelRepository.findById(quote.getEntityId())
                    .ifPresent(entity -> {
                        entity.setStatus(STATUS_CONFIRMED);
                        tripHotelRepository.save(entity);
                    });
            case "attraction" -> tripAttractionRepository.findById(quote.getEntityId())
                    .ifPresent(entity -> {
                        entity.setStatus(STATUS_CONFIRMED);
                        tripAttractionRepository.save(entity);
                    });
            default -> log.debug("No status update applied for productType={}", quote.getProductType());
        }
    }

    /**
     * Persists a failure record when quoting fails.
     */
    private void persistFailure(Long tripId, String productType, Long entityId, String details) {
        String errorDetails = details != null ? details : "Booking request failed";
        TripBookingQuote quote = tripBookingQuoteRepository.findByTripIdAndEntityIdAndProductType(tripId, entityId, productType)
                .orElse(TripBookingQuote.builder()
                        .tripId(tripId)
                        .productType(productType)
                        .entityId(entityId)
                        .itemReference(buildReference(productType, entityId))
                        .build());
        quote.setStatus(STATUS_FAILED);
        quote.setVoucherCode(null);
        quote.setInvoiceId(null);
        quote.setRawResponse(errorDetails);
        tripBookingQuoteRepository.save(quote);
        log.warn("Stored failed booking quote for tripId={}, reference={}, reason={}",
                tripId, quote.getItemReference(), errorDetails);
    }

    /**
     * Normalizes the productType value to lowercase (e.g., 'Transportation' → 'transportation')
     */
    private String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new IllegalArgumentException("productType must not be empty");
        }
        return productType.trim().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Determines if the item should be quoted based on its status and reservationRequired flag.
     */
    private boolean shouldQuote(Boolean reservationRequired, String status) {
        if (reservationRequired == null || !reservationRequired) {
            return false;
        }
        return !STATUS_CONFIRMED.equalsIgnoreCase(status);
    }

    /**
     * Determines party size, defaulting to 1 if not specified.
     */
    private Integer resolvePartySize(TripPreference preference) {
        Integer people = preference.getPeople();
        return (people != null && people > 0) ? people : 1;
    }

    /**
     * Chooses the correct currency for quote, falling back to preference default.
     */
    private String resolveCurrency(String entityCurrency, TripPreference preference) {
        if (StringUtils.hasText(entityCurrency)) {
            return entityCurrency;
        }
        return StringUtils.hasText(preference.getCurrency()) ? preference.getCurrency() : DEFAULT_CURRENCY;
    }

    /**
     * Serializes a response object to JSON string, suppressing any JSON errors.
     */
    private String serializeSafely(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return String.valueOf(data);
        }
    }

    /**
     * Converts OffsetDateTime to Instant safely.
     */
    /**
     * Computes the total amount from all quote items, if present.
     */
    private Integer computeTotalAmount(List<QuoteItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean hasAmount = false;
        for (QuoteItem item : items) {
            BigDecimal itemTotal = item.total();
            if (itemTotal != null) {
                total = total.add(itemTotal);
                hasAmount = true;
            }
        }
        return hasAmount ? toIntegerAmount(total) : null;
    }

    private String resolveItemCurrency(ItineraryQuoteItem item, ItineraryItemContext ctx, String fallbackCurrency) {
        if (item.quoteItems() != null && !item.quoteItems().isEmpty()) {
            QuoteItem firstQuoteItem = item.quoteItems().get(0);
            if (StringUtils.hasText(firstQuoteItem.currency())) {
                return firstQuoteItem.currency();
            }
        }
        if (StringUtils.hasText(ctx.currency())) {
            return ctx.currency();
        }
        return fallbackCurrency;
    }

    private Integer toIntegerAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal normalised = amount.stripTrailingZeros();
        if (normalised.scale() <= 0) {
            return normalised.intValue();
        }
        return normalised.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Constructs the reference string for the given entity and type (e.g., hotel_42).
     */
    private String buildReference(String productType, Long entityId) {
        return switch (productType) {
            case "transportation" -> "transport_" + entityId;
            case "hotel" -> "hotel_" + entityId;
            case "attraction" -> "attraction_" + entityId;
            default -> productType + "_" + entityId;
        };
    }

    /**
     * Infers the mode of transportation from the provider string (e.g., 'ANA' → 'flight').
     */
    private String inferTransportationMode(String provider) {
        if (!StringUtils.hasText(provider)) {
            return "bus";
        }
        String lower = provider.toLowerCase(Locale.ENGLISH);
        if (lower.contains("air") || lower.contains("airline") || lower.contains("jet") || lower.contains("qantas")) {
            return "flight";
        }
        if (lower.contains("rail") || lower.contains("train")) {
            return "train";
        }
        if (lower.contains("bus")) {
            return "bus";
        }
        return "bus";
    }

    /**
     * Returns fallback value if the primary string is blank.
     */
    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record QuotePayload(String productType,
                                Long entityId,
                                Map<String, Object> params,
                                String currency,
                                String reference) {
    }

    record ItineraryItemContext(String productType,
                                Long entityId,
                                String reference,
                                Map<String, Object> params,
                                String currency) {
    }
}
