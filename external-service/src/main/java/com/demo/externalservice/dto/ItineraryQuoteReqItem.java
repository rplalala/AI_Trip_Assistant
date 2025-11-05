package com.demo.externalservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record ItineraryQuoteReqItem(
        @NotBlank
        String reference,

        @NotBlank
        @JsonProperty("product_type")
        String productType,

        @NotNull
        @Positive
        @JsonProperty("party_size")
        Integer partySize,

        @NotNull
        Map<String, Object> params,

        @JsonProperty("entity_id")
        Long entityId
) {
}
