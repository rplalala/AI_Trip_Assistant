package com.demo.externalservice.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ItineraryQuoteResp(
        @NotBlank
        @JsonProperty("quote_token")
        String quoteToken,

        @NotNull
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,

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
