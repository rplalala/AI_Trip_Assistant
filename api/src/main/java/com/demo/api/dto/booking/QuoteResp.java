package com.demo.api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record QuoteResp(
        @NotBlank
        @JsonProperty("quote_token")
        String quoteToken,

        @NotNull
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,

        @NotNull
        @NotEmpty
        List<QuoteItem> items
) {
}
