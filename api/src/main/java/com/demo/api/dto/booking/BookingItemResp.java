package com.demo.api.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * View model representing a reservation-required trip item and the data
 * needed for confirming it via the booking endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingItemResp(
        Long entityId,
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
        QuotePayload quoteRequest,
        QuoteSummary quoteSummary
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuotePayload(
            String productType,
            String currency,
            Integer partySize,
            Map<String, Object> params,
            Long tripId,
            Long entityId,
            String itemReference
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuoteSummary(
            String voucherCode,
            String invoiceId,
            String status,
            String currency,
            Integer totalAmount
    ) {
    }
}
