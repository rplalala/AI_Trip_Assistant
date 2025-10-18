package com.demo.externalservice.exception;

public class QuoteTokenInvalidException extends RuntimeException {
    public QuoteTokenInvalidException(String message) {
        super(message);
    }

    public QuoteTokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
