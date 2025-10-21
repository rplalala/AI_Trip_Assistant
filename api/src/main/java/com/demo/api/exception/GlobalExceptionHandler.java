package com.demo.api.exception;

import com.demo.api.ApiRespond;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Optional;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private record ExternalError(String code, String message) {}

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiRespond<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ApiRespond.error("System Wrong: " + e.getMessage());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiRespond<Void> handleBusinessException(BusinessException e) {
        return ApiRespond.error(e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiRespond<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiRespond.error("Parameter Error: " + e.getMessage());
    }

    /**
     * 处理 @Valid 校验失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiRespond<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Parameter Verification Failed");
        return ApiRespond.error(msg);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiRespond<Void> handleAuthException(AuthException e) {
        return ApiRespond.error(e.getMessage());
    }

    /**
     * 处理权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiRespond<Void> handleAccessDenied(AccessDeniedException e) {
        return ApiRespond.error("No Access");
    }

    /**
     * Handling exceptions when calling external services (Booking Service)
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiRespond<Void>> handleFeignException(FeignException e) {
        ExternalError externalError = parseExternalError(e);
        HttpStatus status = mapStatus(externalError, e.status());
        String message = mapMessage(externalError);
        log.warn("Booking service error: status={}, code={}, message={}", status, externalError.code(), externalError.message());
        return ResponseEntity.status(status).body(ApiRespond.error(message));
    }

    private ExternalError parseExternalError(FeignException e) {
        if (e.responseBody().isEmpty()) {
            return new ExternalError(null, e.getMessage());
        }
        try {
            String content = e.contentUTF8();
            if (content == null || content.isBlank()) {
                return new ExternalError(null, e.getMessage());
            }
            JsonNode node = OBJECT_MAPPER.readTree(content);
            String code = Optional.ofNullable(node.path("error_code").textValue())
                    .orElse(node.path("code").textValue());
            String message = Optional.ofNullable(node.path("message").textValue()).orElse(e.getMessage());
            return new ExternalError(code, message);
        } catch (IOException ioException) {
            log.debug("Failed to parse feign error body", ioException);
            return new ExternalError(null, e.getMessage());
        }
    }

    private HttpStatus mapStatus(ExternalError error, int fallbackStatus) {
        if (error.code() != null) {
            return switch (error.code()) {
                case "REQUOTE_REQUIRED", "QUOTE_EXPIRED" -> HttpStatus.CONFLICT;
                case "PAYMENT_FAILED", "PAYMENT_DECLINED" -> HttpStatus.PAYMENT_REQUIRED;
                case "INVALID_QUOTE_TOKEN", "QUOTE_TOKEN_INVALID", "QUOTE_SIGNATURE_INVALID" -> HttpStatus.BAD_REQUEST;
                default -> Optional.ofNullable(HttpStatus.resolve(fallbackStatus)).orElse(HttpStatus.BAD_GATEWAY);
            };
        }
        return Optional.ofNullable(HttpStatus.resolve(fallbackStatus)).orElse(HttpStatus.BAD_GATEWAY);
    }

    private String mapMessage(ExternalError error) {
        if (error.code() == null) {
            return Optional.ofNullable(error.message()).orElse("Booking service unavailable");
        }
        return switch (error.code()) {
            case "REQUOTE_REQUIRED", "QUOTE_EXPIRED" -> "ERR_QUOTE_EXPIRED: " +
                    Optional.ofNullable(error.message()).orElse("Quote expired, please request a new quote.");
            case "PAYMENT_FAILED", "PAYMENT_DECLINED" -> "ERR_PAYMENT_FAILED: " +
                    Optional.ofNullable(error.message()).orElse("Payment could not be completed.");
            case "INVALID_QUOTE_TOKEN", "QUOTE_TOKEN_INVALID", "QUOTE_SIGNATURE_INVALID" -> "ERR_INVALID_QUOTE_TOKEN: " +
                    Optional.ofNullable(error.message()).orElse("Provided quote token is invalid.");
            default -> Optional.ofNullable(error.message()).orElse("Booking service error.");
        };
    }
}
