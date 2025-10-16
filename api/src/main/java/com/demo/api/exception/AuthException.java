package com.demo.api.exception;

public class AuthException extends RuntimeException{
    public AuthException(String message) {
        super(message);
    }
}
