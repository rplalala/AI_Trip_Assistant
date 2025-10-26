package com.demo.api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ItineraryQuoteResp(
        @NotBlank
        @JsonProperty("voucher_code")
        String voucherCode,

        @NotBlank
        @JsonProperty("invoice_id")
        String invoiceId,

        @NotBlank
        String currency,

        @NotNull
        @NotEmpty
        List<ItineraryQuoteItem> items,

        @NotNull
        @JsonProperty("bundle_total")
        BigDecimal bundleTotal,

        @NotNull
        @JsonProperty("bundle_fees")
        BigDecimal bundleFees
) {
    public ItineraryQuoteResp {
        items = List.copyOf(items);
    }
}
