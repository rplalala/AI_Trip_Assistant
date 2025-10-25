package com.demo.api.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.demo.api.client.BookingApiClient;
import com.demo.api.dto.booking.ConfirmReq;
import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteReqItem;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteItem;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.exception.BookingApiException;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripBookingQuote;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripPreference;
import com.demo.api.model.TripTransportation;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripBookingQuoteRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripPreferenceRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.demo.api.service.BookingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service implementation for handling booking logic via external Booking API.
 * Supports quoting individual items, full itineraries, and confirming bookings.
 */
@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_FAILED = "failed";
    private static final String DEFAULT_CURRENCY = "AUD";
    private static final String DEFAULT_PAYMENT_TOKEN = "pm_mock_4242";

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
                payload.params()
        );

        try {
            QuoteResp response = bookingApiClient.postQuote(request);
            TripBookingQuote saved = upsertQuoteRecord(
                    tripId,
                    normalizedType,
                    entityId,
                    response.quoteToken(),
                    payload.currency(),
                    computeTotalAmount(response.items()),
                    toInstant(response.expiresAt()),
                    serializeSafely(response),
                    "quoted"
            );
            log.info("Stored booking quote for tripId={}, reference={}, token={}",
                    tripId, saved.getItemReference(), response.quoteToken());
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
                .map(ctx -> new ItineraryQuoteReqItem(ctx.reference(), ctx.productType(), resolvePartySize(preference), ctx.params()))
                .collect(Collectors.toList());

        ItineraryQuoteReq request = new ItineraryQuoteReq("iti_" + tripId, requestCurrency, requestItems);

        try {
            ItineraryQuoteResp response = bookingApiClient.postItineraryQuote(request);
            String rawResponse = serializeSafely(response);
            for (QuoteItem item : response.items()) {
                String itemReference = item.itemReference();
                ItineraryItemContext ctx = contexts.stream()
                        .filter(candidate -> candidate.reference().equals(itemReference))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unexpected itinerary item reference: " + itemReference));

                upsertQuoteRecord(
                        tripId,
                        ctx.productType(),
                        ctx.entityId(),
                        response.quoteToken(),
                        item.currency() != null ? item.currency() : ctx.currency(),
                        item.total(),
                        toInstant(response.expiresAt()),
                        rawResponse,
                        "quoted"
                );
            }
            log.info("Stored itinerary quote for tripId={}, token={}", tripId, response.quoteToken());
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

    /**
     * Confirms a booking using a previously issued quoteToken.
     * Updates the quote status and raw response; marks corresponding trip entities as 'confirmed'.
     */
    @Override
    public ConfirmResp confirmBooking(String quoteToken, List<String> itemRefs) {
        Assert.hasText(quoteToken, "quoteToken must not be empty");

        List<String> requestedRefs = itemRefs == null ? List.of() : List.copyOf(itemRefs);
        List<TripBookingQuote> quotesToUpdate = resolveQuotesForConfirmation(quoteToken, requestedRefs);
        if (quotesToUpdate.isEmpty()) {
            throw new IllegalArgumentException("No booking quotes found for token " + quoteToken);
        }

        ConfirmReq confirmReq = new ConfirmReq(quoteToken, DEFAULT_PAYMENT_TOKEN, requestedRefs);
        String idempotencyKey = UUID.randomUUID().toString();

        try {
            ConfirmResp response = bookingApiClient.postConfirm(confirmReq, idempotencyKey);
            String rawResponse = serializeSafely(response);
            quotesToUpdate.forEach(quote -> {
                quote.setStatus(STATUS_CONFIRMED);
                quote.setRawResponse(rawResponse);
            });
            tripBookingQuoteRepository.saveAll(quotesToUpdate);
            quotesToUpdate.forEach(this::markEntityConfirmed);
            log.info("Booking confirmed for token={}, updated {} items", quoteToken, quotesToUpdate.size());
            return response;
        } catch (BookingApiException ex) {
            markQuotesFailed(quotesToUpdate, ex.getResponseBody() != null ? ex.getResponseBody() : ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markQuotesFailed(quotesToUpdate, ex.getMessage());
            throw ex;
        }
    }

    private TripPreference fetchPreference(Long tripId) {
        return tripPreferenceRepository.findByTripId(tripId)
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
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getTripId()),
                    "Transportation does not belong to trip " + preference.getTripId());
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
        return new QuotePayload("transportation", entity.getId(), params, resolveCurrency(entity.getCurrency(), preference));
    }

    /**
     * Builds the 'params' map for hotel quote based on TripHotel entity.
     * Includes fields like hotelName, checkIn, checkOut, roomType, stars.
     */
    private QuotePayload buildHotelPayload(Long entityId, TripPreference preference, boolean validateTrip) {
        TripHotel entity = tripHotelRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getTripId()),
                    "Hotel does not belong to trip " + preference.getTripId());
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
        return new QuotePayload("hotel", entity.getId(), params, resolveCurrency(entity.getCurrency(), preference));
    }

    /**
     * Builds the 'params' map for attraction quote based on TripAttraction entity.
     * Includes fields like name, city, date, session, ticketPrice.
     */
    private QuotePayload buildAttractionPayload(Long entityId, TripPreference preference, boolean validateTrip) {
        TripAttraction entity = tripAttractionRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Attraction not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getTripId()),
                    "Attraction does not belong to trip " + preference.getTripId());
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
        return new QuotePayload("attraction", entity.getId(), params, resolveCurrency(entity.getCurrency(), preference));
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

    /**
     * Marks the corresponding entity (hotel/transportation/attraction) as confirmed in DB.
     */
    private void markEntityConfirmed(TripBookingQuote quote) {
        switch (quote.getProductType()) {
            case "transportation" -> tripTransportationRepository.findById(quote.getEntityId()).ifPresent(entity -> {
                entity.setStatus(STATUS_CONFIRMED);
                tripTransportationRepository.save(entity);
            });
            case "hotel" -> tripHotelRepository.findById(quote.getEntityId()).ifPresent(entity -> {
                entity.setStatus(STATUS_CONFIRMED);
                tripHotelRepository.save(entity);
            });
            case "attraction" -> tripAttractionRepository.findById(quote.getEntityId()).ifPresent(entity -> {
                entity.setStatus(STATUS_CONFIRMED);
                tripAttractionRepository.save(entity);
            });
            default -> log.warn("Unknown product type {} for confirmation", quote.getProductType());
        }
    }

    /**
     * Resolves and returns all booking quotes for a given quoteToken and optional itemRefs.
     * If itemRefs is empty, returns all quotes with the given token.
     */
    private List<TripBookingQuote> resolveQuotesForConfirmation(String quoteToken, List<String> itemRefs) {
        if (itemRefs.isEmpty()) {
            return tripBookingQuoteRepository.findByQuoteToken(quoteToken);
        }
        return itemRefs.stream()
                .map(ref -> tripBookingQuoteRepository.findByQuoteTokenAndItemReference(quoteToken, ref)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Quote not found for token " + quoteToken + " and reference " + ref)))
                .collect(Collectors.toList());
    }

    /**
     * Inserts or updates a TripBookingQuote record based on quoteToken + item info.
     */
    private TripBookingQuote upsertQuoteRecord(Long tripId,
                                               String productType,
                                               Long entityId,
                                               String quoteToken,
                                               String currency,
                                               Integer totalAmount,
                                               Instant expiresAt,
                                               String rawResponse,
                                               String status) {
        TripBookingQuote quote = tripBookingQuoteRepository.findByTripIdAndEntityId(tripId, entityId)
                .orElseGet(() -> TripBookingQuote.builder()
                        .tripId(tripId)
                        .productType(productType)
                        .entityId(entityId)
                        .itemReference(buildReference(productType, entityId))
                        .build());

        quote.setProductType(productType);
        quote.setItemReference(buildReference(productType, entityId));
        quote.setQuoteToken(quoteToken);
        quote.setCurrency(currency);
        quote.setTotalAmount(totalAmount);
        quote.setExpiresAt(expiresAt);
        quote.setRawResponse(rawResponse);
        quote.setStatus(status);
        return tripBookingQuoteRepository.save(quote);
    }

    /**
     * Persists a failure record when quoting or confirmation fails.
     */
    private void persistFailure(Long tripId, String productType, Long entityId, String details) {
        String errorDetails = details != null ? details : "Booking request failed";
        TripBookingQuote quote = tripBookingQuoteRepository.findByTripIdAndEntityId(tripId, entityId)
                .orElse(TripBookingQuote.builder()
                        .tripId(tripId)
                        .productType(productType)
                        .entityId(entityId)
                        .itemReference(buildReference(productType, entityId))
                        .build());
        quote.setStatus(STATUS_FAILED);
        quote.setRawResponse(errorDetails);
        tripBookingQuoteRepository.save(quote);
        log.warn("Stored failed booking quote for tripId={}, reference={}, reason={}",
                tripId, quote.getItemReference(), errorDetails);
    }

    private void markQuotesFailed(List<TripBookingQuote> quotes, String details) {
        if (quotes.isEmpty()) {
            return;
        }
        String failure = details != null ? details : "Booking confirmation failed";
        quotes.forEach(quote -> {
            quote.setStatus(STATUS_FAILED);
            quote.setRawResponse(failure);
        });
        tripBookingQuoteRepository.saveAll(quotes);
    }

    private String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new IllegalArgumentException("productType must not be empty");
        }
        return productType.trim().toLowerCase(Locale.ENGLISH);
    }

    private boolean shouldQuote(Boolean reservationRequired, String status) {
        if (reservationRequired == null || !reservationRequired) {
            return false;
        }
        return !STATUS_CONFIRMED.equalsIgnoreCase(status);
    }

    private Integer resolvePartySize(TripPreference preference) {
        Integer people = preference.getPeople();
        return (people != null && people > 0) ? people : 1;
    }

    private String resolveCurrency(String entityCurrency, TripPreference preference) {
        if (StringUtils.hasText(entityCurrency)) {
            return entityCurrency;
        }
        return StringUtils.hasText(preference.getCurrency()) ? preference.getCurrency() : DEFAULT_CURRENCY;
    }

    private String serializeSafely(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return String.valueOf(data);
        }
    }

    private Instant toInstant(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.toInstant() : null;
    }

    private Integer computeTotalAmount(List<QuoteItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        int total = 0;
        boolean hasTotal = false;
        for (QuoteItem item : items) {
            Integer itemTotal = item.total();
            if (itemTotal != null) {
                total += itemTotal;
                hasTotal = true;
            }
        }
        return hasTotal ? total : null;
    }

    private String buildReference(String productType, Long entityId) {
        return switch (productType) {
            case "transportation" -> "transport_" + entityId;
            case "hotel" -> "hotel_" + entityId;
            case "attraction" -> "attraction_" + entityId;
            default -> productType + "_" + entityId;
        };
    }

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

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record QuotePayload(String productType,
                                Long entityId,
                                Map<String, Object> params,
                                String currency) {
    }

    record ItineraryItemContext(String productType,
                                Long entityId,
                                String reference,
                                Map<String, Object> params,
                                String currency) {
    }
}
