package com.demo.externalservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Map;

public record QuoteItem(
        @NotBlank
        String sku,

        @NotNull
        @PositiveOrZero
        @JsonProperty("unit_price")
        BigDecimal unitPrice,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @PositiveOrZero
        BigDecimal fees,

        @NotNull
        @PositiveOrZero
        BigDecimal total,

        @NotBlank
        String currency,

        Map<String, Object> meta,

        @JsonProperty("cancellation_policy")
        String cancellationPolicy
) {
}
