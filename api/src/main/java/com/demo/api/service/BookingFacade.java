package com.demo.api.service;

import com.demo.api.client.BookingClient;
import com.demo.api.dto.booking.ConfirmReq;
import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingFacade {

    private final BookingClient bookingClient;

    public QuoteResp quote(QuoteReq request, String userId) {
        log.debug("Submitting booking quote for user={}, productType={}", userId, request.productType());
        return bookingClient.quote(request);
    }

    public ItineraryQuoteResp prepareItinerary(ItineraryQuoteReq request, String userId) {
        log.info("Preparing itinerary quote for user={}, itineraryId={}, items={}",
                userId, request.itineraryId(), request.items().size());
        return bookingClient.itineraryQuote(request);
    }

    public ConfirmResp confirm(ConfirmReq request, String userId, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            log.debug("Booking confirm requested with Idempotency-Key={}, user={}", idempotencyKey, userId);
        } else {
            log.debug("Booking confirm requested without Idempotency-Key, user={}", userId);
        }
        if (request.hasItemSelection()) {
            log.info("User={} confirming quote={} with selected items {}", userId, request.quoteToken(), request.itemRefs());
        }
        ConfirmResp response = bookingClient.confirm(idempotencyKey, request);
        if ("CONFIRMED".equalsIgnoreCase(response.status())) {
            // TODO persist confirmed booking details (status, voucher, invoice) in domain repository
            log.info("Booking confirmed for user={}, voucherCode={}, invoiceId={}", userId, response.voucherCode(), response.invoiceId());
        }
        return response;
    }
}
