package com.demo.externalservice.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ConfirmReq(
        @NotBlank
        @JsonProperty("quote_token")
        String quoteToken,

        @NotBlank
        @JsonProperty("payment_token")
        String paymentToken,

        @JsonProperty("item_refs")
        List<String> itemRefs
) {
    public ConfirmReq {
        itemRefs = itemRefs == null ? List.of() : List.copyOf(itemRefs);
    }

    public boolean hasItemSelection() {
        return !itemRefs.isEmpty();
    }
}
