package com.demo.externalservice.exception;

public class PaymentTokenException extends RuntimeException {
    public PaymentTokenException(String message) {
        super(message);
    }
}
