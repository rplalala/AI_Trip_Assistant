package com.demo.api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuoteResp(
        @NotBlank
        @JsonProperty("voucher_code")
        String voucherCode,

        @NotBlank
        @JsonProperty("invoice_id")
        String invoiceId,

        @NotNull
        @NotEmpty
        List<QuoteItem> items
) {
}
