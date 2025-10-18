package com.demo.externalservice.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfirmResp(
        String status,
        @JsonProperty("voucher_code")
        String voucherCode,
        @JsonProperty("invoice_id")
        String invoiceId
) {
}
