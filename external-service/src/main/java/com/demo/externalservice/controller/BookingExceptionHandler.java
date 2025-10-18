package com.demo.externalservice.controller;

import com.demo.externalservice.dto.booking.ErrorResp;
import com.demo.externalservice.exception.IdempotencyMismatchException;
import com.demo.externalservice.exception.PaymentFailedException;
import com.demo.externalservice.exception.PaymentTokenException;
import com.demo.externalservice.exception.QuoteExpiredException;
import com.demo.externalservice.exception.QuoteTokenInvalidException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BookingExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResp> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResp("ERR_VALIDATION", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResp> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResp("ERR_VALIDATION", ex.getMessage()));
    }

    @ExceptionHandler(QuoteExpiredException.class)
    public ResponseEntity<ErrorResp> handleQuoteExpired(QuoteExpiredException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResp("ERR_QUOTE_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(QuoteTokenInvalidException.class)
    public ResponseEntity<ErrorResp> handleInvalidToken(QuoteTokenInvalidException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResp("ERR_INVALID_QUOTE_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResp> handlePaymentFailed(PaymentFailedException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ErrorResp("ERR_PAYMENT_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(PaymentTokenException.class)
    public ResponseEntity<ErrorResp> handlePaymentToken(PaymentTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResp("ERR_PAYMENT_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyMismatchException.class)
    public ResponseEntity<ErrorResp> handleIdempotencyMismatch(IdempotencyMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResp("ERR_IDEMPOTENCY_MISMATCH", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResp> handleOther(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("ERR_INTERNAL", ex.getMessage()));
    }
}
