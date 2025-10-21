package com.demo.externalservice.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record QuoteResp(
        @JsonProperty("quote_token")
        String quoteToken,
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,
        List<QuoteItem> items
) {
}
