package com.demo.api.service.impl;

import com.demo.api.client.BookingClient;
import com.demo.api.dto.booking.*;
import com.demo.api.exception.BookingApiException;
import com.demo.api.exception.ConflictException;
import com.demo.api.model.*;
import com.demo.api.repository.*;
import com.demo.api.service.BookingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

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

    private final BookingClient bookingClient;
    private final TripTransportationRepository tripTransportationRepository;
    private final TripHotelRepository tripHotelRepository;
    private final TripAttractionRepository tripAttractionRepository;
    private final TripBookingQuoteRepository tripBookingQuoteRepository;
    private final TripRepository tripRepository;
    private final ObjectMapper objectMapper;

    public BookingServiceImpl(BookingClient bookingClient,
                              TripTransportationRepository tripTransportationRepository,
                              TripHotelRepository tripHotelRepository,
                              TripAttractionRepository tripAttractionRepository,
                              TripBookingQuoteRepository tripBookingQuoteRepository,
                              TripRepository tripRepository,
                              ObjectMapper objectMapper) {
        this.bookingClient = bookingClient;
        this.tripTransportationRepository = tripTransportationRepository;
        this.tripHotelRepository = tripHotelRepository;
        this.tripAttractionRepository = tripAttractionRepository;
        this.tripBookingQuoteRepository = tripBookingQuoteRepository;
        this.tripRepository = tripRepository;
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
        Trip preference = fetchPreference(tripId);
        String normalizedType = normalizeProductType(productType);
        log.debug("Preparing single quote for tripId={}, productType={}, entityId={}", tripId, normalizedType, entityId);

        // Prevent duplicate booking for already-confirmed item
        if (isAlreadyConfirmed(normalizedType, entityId)) {
            throw new ConflictException("Booking already confirmed for tripId=" + tripId + ", entityId=" + entityId);
        }

        QuotePayload payload = buildQuotePayload(normalizedType, entityId, preference, true);
        String apiProductType = mapToApiProductType(normalizedType);
        QuoteReq request = new QuoteReq(
                apiProductType,
                payload.currency(),
                payload.partySize() != null ? payload.partySize() : resolvePartySize(preference),
                payload.params(),
                tripId,
                entityId,
                payload.reference()
        );

        try {
            // Send booking request to external Booking API via Feign Client
            QuoteResp response = bookingClient.quote(request);
            // Persist successful quote and mark entity as confirmed
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
        } catch (FeignException ex) {
            BookingApiException wrapped = wrapFeignException("/quote", ex);
            persistFailure(tripId, normalizedType, entityId, wrapped.getResponseBody() != null ? wrapped.getResponseBody() : wrapped.getMessage());
            throw wrapped;
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
        Trip preference = fetchPreference(tripId);

        List<ItineraryItemContext> contexts = collectPendingItems(tripId, preference);
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("No pending reservation-required items for trip " + tripId);
        }

        String requestCurrency = preference.getCurrency() != null ? preference.getCurrency() : DEFAULT_CURRENCY;
        List<ItineraryQuoteReqItem> requestItems = contexts.stream()
                .map(ctx -> new ItineraryQuoteReqItem(
                        ctx.reference(),
                        mapToApiProductType(ctx.productType()),
                        ctx.partySize() != null ? ctx.partySize() : resolvePartySize(preference),
                        ctx.params(),
                        ctx.entityId()
                ))
                .collect(Collectors.toList());

        ItineraryQuoteReq request = new ItineraryQuoteReq("iti_" + tripId, requestCurrency, requestItems, tripId);

        try {
            // Send bundled itinerary quote to external Booking API
            ItineraryQuoteResp response = bookingClient.itineraryQuote(request);
            String rawResponse = serializeSafely(response);
            for (ItineraryQuoteItem item : response.items()) {
                String itemReference = item.reference();
                ItineraryItemContext ctx = contexts.stream()
                        .filter(candidate -> candidate.reference().equals(itemReference))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unexpected itinerary item reference: " + itemReference));
                String itemCurrency = resolveItemCurrency(item, ctx, response.currency());

                // Persist successful quote and mark entity as confirmed
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
        } catch (FeignException ex) {
            BookingApiException wrapped = wrapFeignException("/itinerary/quote", ex);
            String errorPayload = wrapped.getResponseBody() != null ? wrapped.getResponseBody() : wrapped.getMessage();
            contexts.forEach(ctx -> persistFailure(tripId, ctx.productType(), ctx.entityId(), errorPayload));
            throw wrapped;
        } catch (RuntimeException ex) {
            contexts.forEach(ctx -> persistFailure(tripId, ctx.productType(), ctx.entityId(), ex.getMessage()));
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingItemResp> listBookingItems(Long tripId, Long userId) {
        Assert.notNull(tripId, "tripId must not be null");
        Trip preference = fetchPreference(tripId);
        if (userId != null && !Objects.equals(preference.getUserId(), userId)) {
            throw new AccessDeniedException("Trip %d does not belong to user %d".formatted(tripId, userId));
        }

        Map<String, TripBookingQuote> quotesByReference = tripBookingQuoteRepository.findByTripId(tripId).stream()
                .collect(Collectors.toMap(
                        quote -> buildReference(quote.getProductType(), quote.getEntityId()),
                        Function.identity(),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ));

        List<BookingItemResp> items = new ArrayList<>();

        tripTransportationRepository.findByTripId(tripId).forEach(entity -> {
            if (Boolean.FALSE.equals(entity.getReservationRequired())) {
                return;
            }
            QuotePayload payload = buildTransportationPayload(entity.getId(), preference, false);
            TripBookingQuote quote = quotesByReference.get(buildReference("transportation", entity.getId()));
            items.add(toTransportationBooking(preference, entity, payload, quote));
        });

        tripHotelRepository.findByTripId(tripId).forEach(entity -> {
            if (Boolean.FALSE.equals(entity.getReservationRequired())) {
                return;
            }
            QuotePayload payload = buildHotelPayload(entity.getId(), preference, false);
            TripBookingQuote quote = quotesByReference.get(buildReference("hotel", entity.getId()));
            items.add(toHotelBooking(preference, entity, payload, quote));
        });

        tripAttractionRepository.findByTripId(tripId).forEach(entity -> {
            if (Boolean.FALSE.equals(entity.getReservationRequired())) {
                return;
            }
            QuotePayload payload = buildAttractionPayload(entity.getId(), preference, false);
            TripBookingQuote quote = quotesByReference.get(buildReference("attraction", entity.getId()));
            items.add(toAttractionBooking(preference, entity, payload, quote));
        });

        items.sort(Comparator
                .comparing(BookingItemResp::date, Comparator.nullsLast(String::compareTo))
                .thenComparing(BookingItemResp::time, Comparator.nullsLast(String::compareTo))
                .thenComparing(BookingItemResp::productType, Comparator.nullsLast(String::compareTo)));

        return List.copyOf(items);
    }

    private BookingApiException wrapFeignException(String path, FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        String body;
        try {
            body = ex.contentUTF8();
        } catch (RuntimeException ignored) {
            body = null;
        }
        String message = String.format("Booking API call to %s failed with status %s", path, status != null ? status : ex.status());
        return new BookingApiException(message, status, body, ex);
    }

    private Trip fetchPreference(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip preference not found for tripId " + tripId));
    }

    private BookingItemResp toTransportationBooking(Trip preference,
                                                    TripTransportation entity,
                                                    QuotePayload payload,
                                                    TripBookingQuote quote) {
        String origin = defaultString(entity.getFrom(), defaultString(preference.getFromCity(), "Origin"));
        String destination = defaultString(entity.getTo(), defaultString(preference.getToCity(), "Destination"));
        String title = StringUtils.hasText(entity.getTitle()) ? entity.getTitle() : origin + " → " + destination;
        String subtitle = StringUtils.hasText(entity.getProvider()) ? entity.getProvider() : null;

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfHasText(metadata, "from", entity.getFrom());
        putIfHasText(metadata, "to", entity.getTo());
        putIfHasText(metadata, "provider", entity.getProvider());
        putIfHasText(metadata, "ticketType", entity.getTicketType());
        if (entity.getPrice() != null) {
            metadata.put("priceEstimate", entity.getPrice());
        }
        putIfHasText(metadata, "currencyHint", entity.getCurrency());

        String currency = firstNonBlank(
                payload.currency(),
                entity.getCurrency(),
                quote != null ? quote.getCurrency() : null
        );

        return buildBookingItem(
                entity.getId(),
                preference.getId(),
                "transportation",
                title,
                subtitle,
                formatDate(entity.getDate()),
                entity.getTime(),
                normalizeStatus(entity.getStatus()),
                entity.getReservationRequired(),
                entity.getPrice(),
                currency,
                entity.getImageUrl(),
                metadata,
                payload,
                quote
        );
    }

    private BookingItemResp toHotelBooking(Trip preference,
                                           TripHotel entity,
                                           QuotePayload payload,
                                           TripBookingQuote quote) {
        String title = StringUtils.hasText(entity.getTitle())
                ? entity.getTitle()
                : (StringUtils.hasText(entity.getHotelName()) ? entity.getHotelName() : "Hotel stay");
        String subtitle = StringUtils.hasText(entity.getHotelName()) ? entity.getHotelName() : null;

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfHasText(metadata, "hotelName", entity.getHotelName());
        putIfHasText(metadata, "roomType", entity.getRoomType());
        if (entity.getNights() != null) {
            metadata.put("nights", entity.getNights());
        }
        if (entity.getPeople() != null) {
            metadata.put("people", entity.getPeople());
        }
        if (entity.getPrice() != null) {
            metadata.put("priceEstimate", entity.getPrice());
        }
        putIfHasText(metadata, "currencyHint", entity.getCurrency());

        String currency = firstNonBlank(
                payload.currency(),
                entity.getCurrency(),
                quote != null ? quote.getCurrency() : null
        );

        return buildBookingItem(
                entity.getId(),
                preference.getId(),
                "hotel",
                title,
                subtitle,
                formatDate(entity.getDate()),
                entity.getTime(),
                normalizeStatus(entity.getStatus()),
                entity.getReservationRequired(),
                entity.getPrice(),
                currency,
                entity.getImageUrl(),
                metadata,
                payload,
                quote
        );
    }

    private BookingItemResp toAttractionBooking(Trip preference,
                                                TripAttraction entity,
                                                QuotePayload payload,
                                                TripBookingQuote quote) {
        String title = StringUtils.hasText(entity.getTitle())
                ? entity.getTitle()
                : defaultString(entity.getLocation(), defaultString(preference.getToCity(), "Attraction"));
        String subtitle = StringUtils.hasText(entity.getLocation()) ? entity.getLocation() : null;

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfHasText(metadata, "location", entity.getLocation());
        if (entity.getTicketPrice() != null) {
            metadata.put("ticketPrice", entity.getTicketPrice());
        }
        if (entity.getPeople() != null) {
            metadata.put("people", entity.getPeople());
        }
        putIfHasText(metadata, "currencyHint", entity.getCurrency());

        String currency = firstNonBlank(
                payload.currency(),
                entity.getCurrency(),
                quote != null ? quote.getCurrency() : null
        );

        return buildBookingItem(
                entity.getId(),
                preference.getId(),
                "attraction",
                title,
                subtitle,
                formatDate(entity.getDate()),
                entity.getTime(),
                normalizeStatus(entity.getStatus()),
                entity.getReservationRequired(),
                entity.getTicketPrice(),
                currency,
                entity.getImageUrl(),
                metadata,
                payload,
                quote
        );
    }

    private BookingItemResp buildBookingItem(Long entityId,
                                             Long tripId,
                                             String productType,
                                             String title,
                                             String subtitle,
                                             String date,
                                             String time,
                                             String status,
                                             Boolean reservationRequired,
                                             Integer price,
                                             String currency,
                                             String imageUrl,
                                             Map<String, Object> metadata,
                                             QuotePayload payload,
                                             TripBookingQuote quote) {
        Map<String, Object> metadataView = (metadata != null && !metadata.isEmpty())
                ? Collections.unmodifiableMap(new LinkedHashMap<>(metadata))
                : null;
        return new BookingItemResp(
                entityId,
                tripId,
                productType,
                title,
                subtitle,
                date,
                time,
                status,
                reservationRequired,
                price,
                currency,
                imageUrl,
                metadataView,
                toQuotePayload(tripId, entityId, payload),
                toQuoteSummary(quote)
        );
    }

    private BookingItemResp.QuotePayload toQuotePayload(Long tripId, Long entityId, QuotePayload payload) {
        Map<String, Object> paramsView = payload.params() != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(payload.params()))
                : Collections.emptyMap();
        return new BookingItemResp.QuotePayload(
                payload.productType(),
                payload.currency(),
                payload.partySize(),
                paramsView,
                tripId,
                entityId,
                payload.reference()
        );
    }

    private BookingItemResp.QuoteSummary toQuoteSummary(TripBookingQuote quote) {
        if (quote == null) {
            return null;
        }
        return new BookingItemResp.QuoteSummary(
                quote.getVoucherCode(),
                quote.getInvoiceId(),
                normalizeStatus(quote.getStatus()),
                quote.getCurrency(),
                quote.getTotalAmount()
        );
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status : "pending";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return DEFAULT_CURRENCY;
    }

    private void putIfHasText(Map<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }

    /**
     * Builds QuotePayload for a given productType (transportation/hotel/attraction),
     * including fallback logic from TripPreference when required.
     */
    private QuotePayload buildQuotePayload(String productType, Long entityId, Trip preference, boolean validateTrip) {
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
    private QuotePayload buildTransportationPayload(Long entityId, Trip preference, boolean validateTrip) {
        TripTransportation entity = tripTransportationRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Transportation not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getId()),
                    "Transportation does not belong to trip " + preference.getId());
        }
        LocalDate date = Objects.requireNonNull(entity.getDate(), "Transportation date is required");
        String currency = resolveCurrency(entity.getCurrency(), preference);
        Map<String, Object> params = new LinkedHashMap<>();
        String from = defaultString(entity.getFrom(), defaultString(preference.getFromCity(), "Unknown origin"));
        String to = defaultString(entity.getTo(), defaultString(preference.getToCity(), "Unknown destination"));
        String ticketType = StringUtils.hasText(entity.getTicketType()) ? entity.getTicketType() : "economy";
        int partySize = resolvePartySize(preference);
        params.put("mode", inferTransportationMode(entity.getProvider()));
        params.put("from", from);
        params.put("to", to);
        params.put("date", date.toString());
        if (StringUtils.hasText(entity.getTime())) {
            params.put("time", entity.getTime());
        }
        params.put("ticket_type", ticketType);
        params.put("class", ticketType);
        if (StringUtils.hasText(entity.getProvider())) {
            params.put("provider", entity.getProvider());
        }
        params.put("status", StringUtils.hasText(entity.getStatus()) ? entity.getStatus() : "pending");
        params.put("reservation_required", entity.getReservationRequired() != null ? entity.getReservationRequired() : Boolean.TRUE);
        params.put("people", partySize);
        params.put("fees", 0);
        if (entity.getPrice() != null) {
            params.put("price", entity.getPrice());
        }
        params.put("currency", currency);
        return new QuotePayload(
                "transportation",
                entity.getId(),
                params,
                currency,
                buildReference("transportation", entity.getId()),
                partySize
        );
    }

    /**
     * Builds the 'params' map for hotel quote based on TripHotel entity.
     * Includes fields like hotelName, checkIn, checkOut, roomType, stars.
     */
    private QuotePayload buildHotelPayload(Long entityId, Trip preference, boolean validateTrip) {
        TripHotel entity = tripHotelRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getId()),
                    "Hotel does not belong to trip " + preference.getId());
        }
        LocalDate checkIn = Objects.requireNonNull(entity.getDate(), "Hotel check-in date is required");
        int nights = entity.getNights() != null && entity.getNights() > 0 ? entity.getNights() : 1;
        String currency = resolveCurrency(entity.getCurrency(), preference);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("city", defaultString(preference.getToCity(), "Unknown city"));
        params.put("date", checkIn.toString());
        params.put("check_in", checkIn.toString());
        params.put("check_out", checkIn.plusDays(nights).toString());
        if (StringUtils.hasText(entity.getTime())) {
            params.put("time", entity.getTime());
        }
        if (StringUtils.hasText(entity.getTitle())) {
            params.put("title", entity.getTitle());
        }
        if (StringUtils.hasText(entity.getHotelName())) {
            params.put("hotel_name", entity.getHotelName());
            params.put("hotelName", entity.getHotelName());
        }
        params.put("nights", nights);
        if (StringUtils.hasText(entity.getRoomType())) {
            params.put("room_type", entity.getRoomType());
            params.put("roomType", entity.getRoomType());
        }
        params.put("stars", 3);
        int partySize = Optional.ofNullable(entity.getPeople()).filter(count -> count > 0)
                .orElse(resolvePartySize(preference));
        params.put("people", partySize);
        if (entity.getPrice() != null) {
            params.put("price", entity.getPrice());
        }
        params.put("fees", 0);
        params.put("status", StringUtils.hasText(entity.getStatus()) ? entity.getStatus() : "pending");
        params.put("reservation_required", entity.getReservationRequired() != null ? entity.getReservationRequired() : Boolean.TRUE);
        params.put("currency", currency);
        return new QuotePayload(
                "hotel",
                entity.getId(),
                params,
                currency,
                buildReference("hotel", entity.getId()),
                partySize
        );
    }

    /**
     * Builds the 'params' map for attraction quote based on TripAttraction entity.
     * Includes fields like name, city, date, session, ticketPrice.
     */
    private QuotePayload buildAttractionPayload(Long entityId, Trip preference, boolean validateTrip) {
        TripAttraction entity = tripAttractionRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Attraction not found: " + entityId));
        if (validateTrip) {
            Assert.isTrue(Objects.equals(entity.getTripId(), preference.getId()),
                    "Attraction does not belong to trip " + preference.getId());
        }
        LocalDate date = Objects.requireNonNull(entity.getDate(), "Attraction date is required");
        String currency = resolveCurrency(entity.getCurrency(), preference);
        Map<String, Object> params = new LinkedHashMap<>();
        String location = StringUtils.hasText(entity.getLocation()) ? entity.getLocation()
                : defaultString(preference.getToCity(), "Unknown city");
        params.put("location", location);
        params.put("city", location);
        if (StringUtils.hasText(entity.getTitle())) {
            params.put("title", entity.getTitle());
            params.put("name", entity.getTitle());
        }
        params.put("date", date.toString());
        if (StringUtils.hasText(entity.getTime())) {
            params.put("time", entity.getTime());
            params.put("session", entity.getTime());
        }
        params.put("status", StringUtils.hasText(entity.getStatus()) ? entity.getStatus() : "pending");
        params.put("reservation_required", entity.getReservationRequired() != null ? entity.getReservationRequired() : Boolean.TRUE);
        int partySize = Optional.ofNullable(entity.getPeople()).filter(count -> count > 0)
                .orElse(resolvePartySize(preference));
        params.put("people", partySize);
        if (entity.getTicketPrice() != null) {
            params.put("ticket_price", entity.getTicketPrice());
            params.put("ticketPrice", entity.getTicketPrice());
            params.put("price", entity.getTicketPrice() * Math.max(1, partySize));
        }
        params.put("fees", 0);
        params.put("currency", currency);
        return new QuotePayload(
                "attraction",
                entity.getId(),
                params,
                currency,
                buildReference("attraction", entity.getId()),
                partySize
        );
    }

    /**
     * Collects all reservation-required items that are still pending confirmation.
     * Builds a context list used to generate itinerary quotes.
     */
    private List<ItineraryItemContext> collectPendingItems(Long tripId, Trip preference) {
        List<ItineraryItemContext> contexts = new ArrayList<>();
        tripTransportationRepository.findByTripId(tripId).stream()
                .filter(item -> shouldQuote(item.getReservationRequired(), item.getStatus()))
                .map(item -> {
                    QuotePayload payload = buildTransportationPayload(item.getId(), preference, false);
                    return new ItineraryItemContext(payload.productType(), payload.entityId(),
                            payload.reference(), payload.params(), payload.currency(), payload.partySize());
                })
                .forEach(contexts::add);

        tripHotelRepository.findByTripId(tripId).stream()
                .filter(item -> shouldQuote(item.getReservationRequired(), item.getStatus()))
                .map(item -> {
                    QuotePayload payload = buildHotelPayload(item.getId(), preference, false);
                    return new ItineraryItemContext(payload.productType(), payload.entityId(),
                            payload.reference(), payload.params(), payload.currency(), payload.partySize());
                })
                .forEach(contexts::add);

        tripAttractionRepository.findByTripId(tripId).stream()
                .filter(item -> shouldQuote(item.getReservationRequired(), item.getStatus()))
                .map(item -> {
                    QuotePayload payload = buildAttractionPayload(item.getId(), preference, false);
                    return new ItineraryItemContext(payload.productType(), payload.entityId(),
                            payload.reference(), payload.params(), payload.currency(), payload.partySize());
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
        String normalizedType = normalizeProductType(productType);
        TripBookingQuote quote = tripBookingQuoteRepository.findByTripIdAndEntityIdAndProductType(tripId, entityId, normalizedType)
                .orElseGet(() -> TripBookingQuote.builder()
                        .tripId(tripId)
                        .productType(normalizedType)
                        .entityId(entityId)
                        .itemReference(buildReference(normalizedType, entityId))
                        .build());

        quote.setProductType(normalizedType);
        quote.setItemReference(buildReference(normalizedType, entityId));
        quote.setVoucherCode(voucherCode);
        quote.setInvoiceId(invoiceId);
        quote.setCurrency(currency);
        quote.setTotalAmount(totalAmount);
        quote.setRawResponse(rawResponse);
        quote.setStatus(status);
        return tripBookingQuoteRepository.save(quote);
    }

    private boolean isAlreadyConfirmed(String productType, Long entityId) {
        String normalizedType = normalizeProductType(productType);
        return switch (normalizedType) {
            case "transportation" -> tripTransportationRepository.findById(entityId)
                    .map(entity -> STATUS_CONFIRMED.equalsIgnoreCase(entity.getStatus()))
                    .orElse(false);
            case "hotel" -> tripHotelRepository.findById(entityId)
                    .map(entity -> STATUS_CONFIRMED.equalsIgnoreCase(entity.getStatus()))
                    .orElse(false);
            case "attraction" -> tripAttractionRepository.findById(entityId)
                    .map(entity -> STATUS_CONFIRMED.equalsIgnoreCase(entity.getStatus()))
                    .orElse(false);
            default -> false;
        };
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
        String normalizedType = normalizeProductType(productType);
        TripBookingQuote quote = tripBookingQuoteRepository.findByTripIdAndEntityIdAndProductType(tripId, entityId, normalizedType)
                .orElse(TripBookingQuote.builder()
                        .tripId(tripId)
                        .productType(normalizedType)
                        .entityId(entityId)
                        .itemReference(buildReference(normalizedType, entityId))
                        .build());
        quote.setProductType(normalizedType);
        quote.setItemReference(buildReference(normalizedType, entityId));
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
        String normalized = productType.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case "transport", "transportation" -> "transportation";
            case "hotel" -> "hotel";
            case "attraction" -> "attraction";
            default -> normalized;
        };
    }

    private String mapToApiProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new IllegalArgumentException("productType must not be empty");
        }
        return switch (productType) {
            case "transportation" -> "transport";
            default -> productType;
        };
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
    private Integer resolvePartySize(Trip preference) {
        Integer people = preference.getPeople();
        return (people != null && people > 0) ? people : 1;
    }

    /**
     * Chooses the correct currency for quote, falling back to preference default.
     */
    private String resolveCurrency(String entityCurrency, Trip preference) {
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
                                String reference,
                                Integer partySize) {
    }

    record ItineraryItemContext(String productType,
                                Long entityId,
                                String reference,
                                Map<String, Object> params,
                                String currency,
                                Integer partySize) {
    }
}
