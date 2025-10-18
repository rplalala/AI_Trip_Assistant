package com.demo.api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record QuoteReq(
        @NotBlank
        @JsonProperty("product_type")
        String productType,

        @NotBlank
        String currency,

        @NotNull
        @Positive
        @JsonProperty("party_size")
        Integer partySize,

        @NotNull
        Map<String, Object> params
) {
}
