package com.demo.api.service;

import com.demo.api.dto.booking.BookingItemResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteItem;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.model.TripBookingQuote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BookingFacade orchestrates controller calls and delegates business logic to BookingService.
 * 
 * Purpose:
 * This class acts as the middle layer between the BookingController (REST endpoint)
 * and the BookingServiceImpl (core business logic).
 * 
 * BookingController → BookingFacade → BookingServiceImpl
 * 
 * Responsibilities:
 * - Orchestrate high-level flow (e.g., log, security context)
 * - Prepare or transform requests if needed
 * - Call BookingServiceImpl for actual quoting logic (validation, persistence, external API)
 * - Convert service-layer result into response DTO (e.g., QuoteResp)
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingFacade {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    /**
     * Handles single-item quote requests from BookingController.
     * This method does NOT contain business logic — it delegates to BookingService.
     *
     * Flow:
     * 1. Receives QuoteReq (from controller)
     * 2. Calls BookingServiceImpl.quoteSingleItem() to handle DB/API logic
     * 3. Converts TripBookingQuote (entity) into QuoteResp (DTO)
     */
    @Transactional
    public QuoteResp quote(QuoteReq request, String userId) {
        log.debug("Submitting booking quote for user={}, productType={}", userId, request.productType());
        // Call the BookingServiceImpl to handle validation + quote + persist logic
        TripBookingQuote quote = bookingService.quoteSingleItem(
                request.tripId(),
                request.productType(),
                request.entityId()
        );
        // Convert TripBookingQuote entity → QuoteResp DTO to return to frontend
        return toQuoteResp(quote);
    }

    /**
     * Converts the DB entity (TripBookingQuote) into the API response (QuoteResp).
     * If raw JSON is available (stored from third-party API), it tries to parse it.
     * If not, fallback to manually building a QuoteResp object from known fields.
     */
    private QuoteResp toQuoteResp(TripBookingQuote quote) {
        if (quote == null) {
            throw new IllegalStateException("Booking quote result is missing after delegation to BookingService");
        }
        String rawResponse = quote.getRawResponse();

        // Prefer deserializing the saved JSON response from Booking API
        if (StringUtils.hasText(rawResponse)) {
            try {
                return objectMapper.readValue(rawResponse, QuoteResp.class);
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize stored quote response for reference {}", quote.getItemReference(), ex);
            }
        }

        // Prefer deserializing the saved JSON response from Booking API
        BigDecimal total = quote.getTotalAmount() != null
                ? BigDecimal.valueOf(quote.getTotalAmount())
                : BigDecimal.ZERO;
        String currency = StringUtils.hasText(quote.getCurrency()) ? quote.getCurrency() : "AUD";

        // Optional meta fields to help frontend rendering
        Map<String, Object> meta = new LinkedHashMap<>();
        if (StringUtils.hasText(quote.getProductType())) {
            meta.put("product_type", quote.getProductType());
        }
        if (StringUtils.hasText(quote.getStatus())) {
            meta.put("status", quote.getStatus());
        }

        // Build a single item quote from DB record
        QuoteItem fallbackItem = new QuoteItem(
                StringUtils.hasText(quote.getItemReference()) ? quote.getItemReference() : "item",
                total,
                1,
                BigDecimal.ZERO,
                total,
                currency,
                meta.isEmpty() ? null : meta,
                "Refer to booking details"
        );

        // Use stored voucher/invoice or default to fallback string
        String voucher = StringUtils.hasText(quote.getVoucherCode()) ? quote.getVoucherCode() : "UNKNOWN_VOUCHER";
        String invoice = StringUtils.hasText(quote.getInvoiceId()) ? quote.getInvoiceId() : "UNKNOWN_INVOICE";
        return new QuoteResp(voucher, invoice, List.of(fallbackItem));
    }

    /**
     * Handles itinerary-level quote request (multi-item).
     * Delegates the full business logic to BookingServiceImpl.
     * 
     * Flow:
     * 1. Receives ItineraryQuoteReq (from controller)
     * 2. Calls BookingServiceImpl.quoteItinerary() to handle filtering, API calls, DB write
     * 3. Returns ItineraryQuoteResp DTO to frontend
     */
    @Transactional
    public ItineraryQuoteResp prepareItinerary(ItineraryQuoteReq request, String userId) {
        log.info("Preparing itinerary quote for user={}, itineraryId={}, items={}",
                userId, request.itineraryId(), request.items().size());

        // Let BookingServiceImpl handle the logic: find quote-needed items, call third-party API, persist results
        return bookingService.quoteItinerary(request.tripId());
    }

    @Transactional(readOnly = true)
    public List<BookingItemResp> listBookings(Long tripId, String userId) {
        Long parsedUserId = parseUserId(userId);
        return bookingService.listBookingItems(tripId, parsedUserId);
    }

    private Long parseUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse userId '{}' as Long", userId);
            return null;
        }
    }
}
