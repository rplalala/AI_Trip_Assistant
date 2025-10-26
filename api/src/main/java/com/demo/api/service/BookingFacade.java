package com.demo.api.service;

import com.demo.api.client.BookingClient;
import com.demo.api.dto.booking.ItineraryQuoteItem;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteReqItem;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteItem;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripBookingQuote;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripTransportation;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripBookingQuoteRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingFacade {

    private static final String STATUS_CONFIRMED = "confirm";

    private final BookingClient bookingClient;
    private final TripBookingQuoteRepository tripBookingQuoteRepository;
    private final TripTransportationRepository tripTransportationRepository;
    private final TripHotelRepository tripHotelRepository;
    private final TripAttractionRepository tripAttractionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuoteResp quote(QuoteReq request, String userId) {
        log.debug("Submitting booking quote for user={}, productType={}", userId, request.productType());
        QuoteReq enrichedRequest = enrichQuoteRequest(request);
        QuoteResp response = bookingClient.quote(enrichedRequest);
        persistSingleQuote(enrichedRequest, response);
        return response;
    }

    @Transactional
    public ItineraryQuoteResp prepareItinerary(ItineraryQuoteReq request, String userId) {
        log.info("Preparing itinerary quote for user={}, itineraryId={}, items={}",
                userId, request.itineraryId(), request.items().size());
        List<ItineraryQuoteReqItem> itemsToQuote = filterItineraryItems(request);
        if (itemsToQuote.isEmpty()) {
            throw new ValidationException("No itinerary items require quoting");
        }
        if (itemsToQuote.size() != request.items().size()) {
            log.info("Filtered out {} itinerary items that do not require quoting",
                    request.items().size() - itemsToQuote.size());
        }
        ItineraryQuoteReq filteredRequest = new ItineraryQuoteReq(
                request.itineraryId(),
                request.currency(),
                itemsToQuote,
                request.tripId()
        );
        ItineraryQuoteReq enrichedRequest = enrichItineraryRequest(filteredRequest);
        ItineraryQuoteResp response = bookingClient.itineraryQuote(enrichedRequest);
        persistItineraryQuotes(filteredRequest, response);
        return response;
    }


    private void persistSingleQuote(QuoteReq request, QuoteResp response) {
        Long tripId = request.tripId();
        Long entityId = request.entityId();
        if (tripId == null || entityId == null) {
            log.debug("Skipping quote persistence for voucher {} – missing tripId/entityId metadata", response.voucherCode());
            return;
        }
        String productType = normalizeProductType(request.productType());
        String reference = resolveReference(request.itemReference(), productType, entityId);

        TripBookingQuote quote = upsertQuoteRecord(
                tripId,
                productType,
                entityId,
                reference
        );
        quote.setVoucherCode(response.voucherCode());
        quote.setInvoiceId(response.invoiceId());
        quote.setCurrency(resolveCurrency(response.items(), request.currency()));
        quote.setTotalAmount(sumTotals(response.items()));
        quote.setRawResponse(serialize(response));
        quote.setStatus(STATUS_CONFIRMED);
        tripBookingQuoteRepository.save(quote);
    }

    private void persistItineraryQuotes(ItineraryQuoteReq request, ItineraryQuoteResp response) {
        Long tripId = request.tripId();
        if (tripId == null) {
            log.debug("Skipping itinerary quote persistence for voucher {} – missing tripId metadata", response.voucherCode());
            return;
        }
        Map<String, ItineraryQuoteReqItem> itemsByRef = request.items().stream()
                .collect(Collectors.toMap(ItineraryQuoteReqItem::reference, item -> item, (left, right) -> left, HashMap::new));

        for (ItineraryQuoteItem item : response.items()) {
            ItineraryQuoteReqItem requestItem = itemsByRef.get(item.reference());
            if (requestItem == null) {
                log.warn("No matching itinerary request item found for reference {} – skipping persistence", item.reference());
                continue;
            }
            Long entityId = requestItem.entityId();
            if (entityId == null) {
                log.debug("Skipping itinerary item {} – missing entityId metadata", item.reference());
                continue;
            }

            String productType = normalizeProductType(item.productType());
            TripBookingQuote quote = upsertQuoteRecord(
                    tripId,
                    productType,
                    entityId,
                    item.reference()
            );
            quote.setVoucherCode(response.voucherCode());
            quote.setInvoiceId(response.invoiceId());
            quote.setCurrency(response.currency());
            quote.setTotalAmount(toIntegerAmount(item.total()));
            quote.setRawResponse(serialize(response));
            quote.setStatus(STATUS_CONFIRMED);
            tripBookingQuoteRepository.save(quote);
        }
    }

    private List<ItineraryQuoteReqItem> filterItineraryItems(ItineraryQuoteReq request) {
        List<ItineraryQuoteReqItem> result = new ArrayList<>(request.items().size());
        for (ItineraryQuoteReqItem item : request.items()) {
            if (shouldQuote(request.tripId(), item)) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean shouldQuote(Long tripId, ItineraryQuoteReqItem item) {
        Long entityId = item.entityId();
        if (entityId == null) {
            return true;
        }
        String productType = normalizeProductType(item.productType());
        return switch (productType) {
            case "hotel" -> shouldQuoteHotel(tripId, entityId, item.reference());
            case "transportation", "transport" -> shouldQuoteTransportation(tripId, entityId, item.reference());
            case "attraction" -> shouldQuoteAttraction(tripId, entityId, item.reference());
            default -> true;
        };
    }

    private boolean shouldQuoteHotel(Long requestTripId, Long entityId, String reference) {
        return tripHotelRepository.findById(entityId)
                .map(hotel -> evaluateQuoteNeed(
                        requestTripId,
                        hotel.getTripId(),
                        hotel.getStatus(),
                        hotel.getReservationRequired(),
                        "hotel",
                        entityId,
                        reference))
                .orElseGet(() -> {
                    log.warn("Hotel entity {} referenced by {} not found while filtering quote request", entityId, reference);
                    return true;
                });
    }

    private boolean shouldQuoteTransportation(Long requestTripId, Long entityId, String reference) {
        return tripTransportationRepository.findById(entityId)
                .map(transport -> evaluateQuoteNeed(
                        requestTripId,
                        transport.getTripId(),
                        transport.getStatus(),
                        transport.getReservationRequired(),
                        "transportation",
                        entityId,
                        reference))
                .orElseGet(() -> {
                    log.warn("Transportation entity {} referenced by {} not found while filtering quote request", entityId, reference);
                    return true;
                });
    }

    private boolean shouldQuoteAttraction(Long requestTripId, Long entityId, String reference) {
        return tripAttractionRepository.findById(entityId)
                .map(attraction -> evaluateQuoteNeed(
                        requestTripId,
                        attraction.getTripId(),
                        attraction.getStatus(),
                        attraction.getReservationRequired(),
                        "attraction",
                        entityId,
                        reference))
                .orElseGet(() -> {
                    log.warn("Attraction entity {} referenced by {} not found while filtering quote request", entityId, reference);
                    return true;
                });
    }

    private boolean evaluateQuoteNeed(Long requestTripId,
                                      Long entityTripId,
                                      String status,
                                      Boolean reservationRequired,
                                      String productType,
                                      Long entityId,
                                      String reference) {
        if (requestTripId != null && entityTripId != null && !Objects.equals(entityTripId, requestTripId)) {
            log.warn("{} entity {} referenced by {} does not belong to trip {}", productType, entityId, reference, requestTripId);
            return true;
        }
        if (Boolean.FALSE.equals(reservationRequired)) {
            log.debug("Skipping {} entity {} ({}) because reservation is not required", productType, entityId, reference);
            return false;
        }
        if (status != null && STATUS_CONFIRMED.equalsIgnoreCase(status)) {
            log.debug("Skipping {} entity {} ({}) because status is already CONFIRMED", productType, entityId, reference);
            return false;
        }
        return true;
    }

    private TripBookingQuote upsertQuoteRecord(Long tripId,
                                               String productType,
                                               Long entityId,
                                               String itemReference) {
        TripBookingQuote quote = tripBookingQuoteRepository.findByTripIdAndEntityIdAndProductType(tripId, entityId, productType)
                .orElseGet(() -> TripBookingQuote.builder()
                        .tripId(tripId)
                        .productType(productType)
                        .entityId(entityId)
                        .itemReference(itemReference)
                        .build());

        quote.setProductType(productType);
        quote.setEntityId(entityId);
        quote.setItemReference(itemReference);
        quote.setTripId(tripId);
        return quote;
    }


    private QuoteReq enrichQuoteRequest(QuoteReq request) {
        Map<String, Object> params = new HashMap<>(request.params());
        String productType = normalizeProductType(request.productType());
        Integer partySize = request.partySize();
        String currency = request.currency();
        if (request.entityId() != null) {
            PricingAdjustment adjustment = applyEntityPricing(productType, request.tripId(), request.entityId(), params, partySize);
            if (adjustment.partySize() != null && adjustment.partySize() > 0) {
                partySize = adjustment.partySize();
            }
            if (StringUtils.hasText(adjustment.currency())) {
                currency = adjustment.currency();
            }
        }
        return new QuoteReq(
                request.productType(),
                currency,
                partySize,
                Map.copyOf(params),
                request.tripId(),
                request.entityId(),
                request.itemReference()
        );
    }

    private ItineraryQuoteReq enrichItineraryRequest(ItineraryQuoteReq request) {
        List<ItineraryQuoteReqItem> items = request.items().stream()
                .map(item -> enrichItineraryItem(request.tripId(), item))
                .toList();
        return new ItineraryQuoteReq(
                request.itineraryId(),
                request.currency(),
                items,
                request.tripId()
        );
    }

    private ItineraryQuoteReqItem enrichItineraryItem(Long tripId, ItineraryQuoteReqItem item) {
        Map<String, Object> params = new HashMap<>(item.params());
        PricingAdjustment adjustment = applyEntityPricing(
                normalizeProductType(item.productType()),
                tripId,
                item.entityId(),
                params,
                item.partySize()
        );
        Integer partySize = adjustment.partySize() != null && adjustment.partySize() > 0
                ? adjustment.partySize()
                : item.partySize();
        return new ItineraryQuoteReqItem(
                item.reference(),
                item.productType(),
                partySize,
                Map.copyOf(params),
                item.entityId()
        );
    }

    private PricingAdjustment applyEntityPricing(String productType,
                                                 Long tripId,
                                                 Long entityId,
                                                 Map<String, Object> params,
                                                 Integer defaultPartySize) {
        return switch (productType) {
            case "hotel" -> applyHotelPricing(tripId, entityId, params, defaultPartySize);
            case "transportation", "transport" -> applyTransportationPricing(tripId, entityId, params, defaultPartySize);
            case "attraction" -> applyAttractionPricing(tripId, entityId, params, defaultPartySize);
            default -> new PricingAdjustment(defaultPartySize, null);
        };
    }

    private PricingAdjustment applyHotelPricing(Long tripId,
                                                Long entityId,
                                                Map<String, Object> params,
                                                Integer defaultPartySize) {
        if (entityId == null) {
            return new PricingAdjustment(defaultPartySize, null);
        }
        Optional<TripHotel> maybeHotel = tripHotelRepository.findById(entityId);
        if (maybeHotel.isEmpty()) {
            log.warn("Hotel entity {} not found while enriching quote request", entityId);
            return new PricingAdjustment(defaultPartySize, null);
        }
        TripHotel hotel = maybeHotel.get();
        if (tripId != null && !Objects.equals(hotel.getTripId(), tripId)) {
            log.warn("Hotel entity {} does not belong to trip {}", entityId, tripId);
            return new PricingAdjustment(defaultPartySize, null);
        }
        if (hotel.getDate() != null) {
            params.put("date", hotel.getDate().toString());
        }
        if (StringUtils.hasText(hotel.getTime())) {
            params.put("time", hotel.getTime());
        }
        if (StringUtils.hasText(hotel.getTitle())) {
            params.put("title", hotel.getTitle());
        }
        if (StringUtils.hasText(hotel.getStatus())) {
            params.put("status", hotel.getStatus());
        }
        if (hotel.getReservationRequired() != null) {
            params.put("reservation_required", hotel.getReservationRequired());
        }
        if (StringUtils.hasText(hotel.getHotelName())) {
            params.put("hotel_name", hotel.getHotelName());
        }
        if (StringUtils.hasText(hotel.getRoomType())) {
            params.put("room_type", hotel.getRoomType());
        }
        if (hotel.getPeople() != null && hotel.getPeople() > 0) {
            params.put("people", hotel.getPeople());
        }
        if (hotel.getNights() != null && hotel.getNights() > 0) {
            params.put("nights", hotel.getNights());
        }
        if (hotel.getPrice() != null) {
            params.put("price", hotel.getPrice());
        }
        String currency = StringUtils.hasText(hotel.getCurrency()) ? hotel.getCurrency() : null;
        Integer partySize = (hotel.getPeople() != null && hotel.getPeople() > 0) ? hotel.getPeople() : defaultPartySize;
        return new PricingAdjustment(partySize, currency);
    }

    private PricingAdjustment applyTransportationPricing(Long tripId,
                                                         Long entityId,
                                                         Map<String, Object> params,
                                                         Integer defaultPartySize) {
        if (entityId == null) {
            return new PricingAdjustment(defaultPartySize, null);
        }
        Optional<TripTransportation> maybeTransport = tripTransportationRepository.findById(entityId);
        if (maybeTransport.isEmpty()) {
            log.warn("Transportation entity {} not found while enriching quote request", entityId);
            return new PricingAdjustment(defaultPartySize, null);
        }
        TripTransportation transport = maybeTransport.get();
        if (tripId != null && !Objects.equals(transport.getTripId(), tripId)) {
            log.warn("Transportation entity {} does not belong to trip {}", entityId, tripId);
            return new PricingAdjustment(defaultPartySize, null);
        }
        if (transport.getDate() != null) {
            params.put("date", transport.getDate().toString());
        }
        if (StringUtils.hasText(transport.getTime())) {
            params.put("time", transport.getTime());
        }
        if (StringUtils.hasText(transport.getTitle())) {
            params.put("title", transport.getTitle());
        }
        if (StringUtils.hasText(transport.getStatus())) {
            params.put("status", transport.getStatus());
        }
        if (transport.getReservationRequired() != null) {
            params.put("reservation_required", transport.getReservationRequired());
        }
        if (StringUtils.hasText(transport.getFrom())) {
            params.put("from", transport.getFrom());
        }
        if (StringUtils.hasText(transport.getTo())) {
            params.put("to", transport.getTo());
        }
        if (StringUtils.hasText(transport.getProvider())) {
            params.put("provider", transport.getProvider());
        }
        if (StringUtils.hasText(transport.getTicketType())) {
            params.put("ticket_type", transport.getTicketType());
        }
        if (transport.getPrice() != null) {
            params.put("price", transport.getPrice());
        }
        String currency = StringUtils.hasText(transport.getCurrency()) ? transport.getCurrency() : null;
        return new PricingAdjustment(defaultPartySize, currency);
    }

    private PricingAdjustment applyAttractionPricing(Long tripId,
                                                     Long entityId,
                                                     Map<String, Object> params,
                                                     Integer defaultPartySize) {
        if (entityId == null) {
            return new PricingAdjustment(defaultPartySize, null);
        }
        Optional<TripAttraction> maybeAttraction = tripAttractionRepository.findById(entityId);
        if (maybeAttraction.isEmpty()) {
            log.warn("Attraction entity {} not found while enriching quote request", entityId);
            return new PricingAdjustment(defaultPartySize, null);
        }
        TripAttraction attraction = maybeAttraction.get();
        if (tripId != null && !Objects.equals(attraction.getTripId(), tripId)) {
            log.warn("Attraction entity {} does not belong to trip {}", entityId, tripId);
            return new PricingAdjustment(defaultPartySize, null);
        }
        if (attraction.getDate() != null) {
            params.put("date", attraction.getDate().toString());
        }
        if (StringUtils.hasText(attraction.getTime())) {
            params.put("time", attraction.getTime());
        }
        if (StringUtils.hasText(attraction.getTitle())) {
            params.put("title", attraction.getTitle());
        }
        if (StringUtils.hasText(attraction.getStatus())) {
            params.put("status", attraction.getStatus());
        }
        if (attraction.getReservationRequired() != null) {
            params.put("reservation_required", attraction.getReservationRequired());
        }
        if (StringUtils.hasText(attraction.getLocation())) {
            params.put("location", attraction.getLocation());
        }
        if (attraction.getPeople() != null && attraction.getPeople() > 0) {
            params.put("people", attraction.getPeople());
        }
        if (attraction.getTicketPrice() != null) {
            params.put("ticket_price", attraction.getTicketPrice());
            params.put("price", attraction.getTicketPrice());
        }
        String currency = StringUtils.hasText(attraction.getCurrency()) ? attraction.getCurrency() : null;
        Integer partySize = (attraction.getPeople() != null && attraction.getPeople() > 0) ? attraction.getPeople() : defaultPartySize;
        return new PricingAdjustment(partySize, currency);
    }

    private record PricingAdjustment(Integer partySize, String currency) {
    }

    private String resolveReference(String providedReference, String productType, Long entityId) {
        if (StringUtils.hasText(providedReference)) {
            return providedReference;
        }
        return buildReference(productType, entityId);
    }

    private String resolveCurrency(List<QuoteItem> items, String fallbackCurrency) {
        Optional<String> firstCurrency = items.stream()
                .map(QuoteItem::currency)
                .filter(StringUtils::hasText)
                .findFirst();
        return firstCurrency.orElse(fallbackCurrency);
    }

    private Integer sumTotals(List<QuoteItem> items) {
        BigDecimal total = items.stream()
                .map(QuoteItem::total)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return toIntegerAmount(total);
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

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize booking payload for persistence", ex);
            return value.toString();
        }
    }

    private String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            return "unknown";
        }
        return productType.toLowerCase(Locale.ENGLISH);
    }

    private String buildReference(String productType, Long entityId) {
        if (entityId == null) {
            return productType;
        }
        return switch (productType) {
            case "transportation" -> "transport_" + entityId;
            case "hotel" -> "hotel_" + entityId;
            case "attraction" -> "attraction_" + entityId;
            default -> productType + "_" + entityId;
        };
    }
}
