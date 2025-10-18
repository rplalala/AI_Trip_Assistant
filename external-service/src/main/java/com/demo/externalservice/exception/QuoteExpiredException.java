package com.demo.externalservice.exception;

public class QuoteExpiredException extends RuntimeException {
    public QuoteExpiredException(String message) {
        super(message);
    }
}
