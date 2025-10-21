package com.demo.externalservice.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ItineraryQuoteItem(
        @NotBlank
        String reference,

        @NotBlank
        @JsonProperty("product_type")
        String productType,

        @NotNull
        @JsonProperty("party_size")
        Integer partySize,

        @NotNull
        @JsonProperty("total")
        BigDecimal total,

        @NotNull
        @JsonProperty("fees")
        BigDecimal fees,

        @NotNull
        @JsonProperty("quote_items")
        List<QuoteItem> quoteItems
) {
    public ItineraryQuoteItem {
        quoteItems = List.copyOf(quoteItems);
    }
}
