package com.demo.externalservice.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ItineraryQuoteReq(
        @NotBlank
        @JsonProperty("itinerary_id")
        String itineraryId,

        @NotBlank
        String currency,

        @NotNull
        @NotEmpty
        @Valid
        List<ItineraryQuoteReqItem> items
) {
    public ItineraryQuoteReq {
        items = List.copyOf(items);
    }
}
