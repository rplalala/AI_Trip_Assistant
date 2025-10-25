package com.demo.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class BookingApiException extends RuntimeException {

    private final HttpStatus status;
    private final String responseBody;

    public BookingApiException(String message, HttpStatus status, String responseBody, Throwable cause) {
        super(message, cause);
        this.status = status != null ? status : HttpStatus.BAD_GATEWAY;
        this.responseBody = responseBody;
    }

    public BookingApiException(String message, HttpStatus status, String responseBody) {
        this(message, status, responseBody, null);
    }

    public BookingApiException(String message, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        this(message, toHttpStatus(statusCode), responseBody, cause);
    }

    public BookingApiException(String message, HttpStatusCode statusCode, String responseBody) {
        this(message, toHttpStatus(statusCode), responseBody, null);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private static HttpStatus toHttpStatus(HttpStatusCode statusCode) {
        if (statusCode == null) {
            return null;
        }
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus;
        }
        return HttpStatus.resolve(statusCode.value());
    }
}
