package com.demo.externalservice.service;

import com.demo.externalservice.exception.PaymentFailedException;
import com.demo.externalservice.exception.PaymentTokenException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class PaymentService {

    public PaymentResult charge(String paymentToken, BigDecimal amount) {
        if (paymentToken == null || paymentToken.isBlank()) {
            throw new PaymentTokenException("Missing payment token");
        }

        if (!paymentToken.startsWith("pm_mock_")) {
            throw new PaymentTokenException("Unsupported payment token");
        }

        int checksum = Math.abs((paymentToken + ":" + amount.toPlainString()).hashCode());
        if (checksum % 20 == 0) { // ~5% deterministic failure
            throw new PaymentFailedException("Payment authorization declined");
        }

        String paymentId = "pay_%s".formatted(Integer.toHexString(checksum).toUpperCase(Locale.ROOT));
        return new PaymentResult(paymentId);
    }

    public record PaymentResult(String paymentId) {
    }
}
