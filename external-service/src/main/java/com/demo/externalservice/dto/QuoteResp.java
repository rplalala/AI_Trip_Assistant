package com.demo.externalservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record QuoteResp(
        @JsonProperty("voucher_code")
        String voucherCode,
        @JsonProperty("invoice_id")
        String invoiceId,
        List<QuoteItem> items
) {
}
