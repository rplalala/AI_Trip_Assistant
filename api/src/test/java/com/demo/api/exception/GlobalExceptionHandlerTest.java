package com.demo.api.exception;

import com.demo.api.ApiRespond;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @DisplayName("handleException wraps unexpected exceptions")
    @Test
    void handleException_returnsGenericError() {
        ApiRespond<Void> resp = handler.handleException(new RuntimeException("disk exploded"));
        assertThat(resp.getCode()).isZero();
        assertThat(resp.getMsg()).contains("System Wrong").contains("disk exploded");
    }

    @DisplayName("Business, auth and validation exceptions map to appropriate messages")
    @Test
    void handleSpecificExceptions() throws NoSuchMethodException {
        ApiRespond<Void> business = handler.handleBusinessException(new BusinessException("failure"));
        assertThat(business.getMsg()).isEqualTo("failure");

        ApiRespond<Void> illegal = handler.handleIllegalArgumentException(new IllegalArgumentException("bad arg"));
        assertThat(illegal.getMsg()).contains("Parameter Error").contains("bad arg");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("trip", "startDate", "must be future")));
        MethodArgumentNotValidException manve = new MethodArgumentNotValidException(null, bindingResult);
        ApiRespond<Void> validation = handler.handleMethodArgumentNotValid(manve);
        assertThat(validation.getMsg()).isEqualTo("startDate: must be future");

        ApiRespond<Void> auth = handler.handleAuthException(new AuthException("auth fail"));
        assertThat(auth.getMsg()).isEqualTo("auth fail");

        ApiRespond<Void> denied = handler.handleAccessDenied(new org.springframework.security.access.AccessDeniedException("nope"));
        assertThat(denied.getMsg()).isEqualTo("No Access");
    }

    @DisplayName("handleFeignException maps known booking errors to friendly messages")
    @Test
    void handleFeignException_knownErrorCode() {
        FeignException ex = feignException(409, Map.of("error_code", "REQUOTE_REQUIRED", "message", "Expired"));
        HttpServletResponse response = mock(HttpServletResponse.class);

        ApiRespond<Void> result = handler.handleFeignException(ex, response);

        verify(response).setStatus(HttpStatus.CONFLICT.value());
        assertThat(result.getMsg()).contains("ERR_QUOTE_EXPIRED").contains("Expired");
    }

    @DisplayName("handleFeignException falls back for missing body or parsing failures")
    @Test
    void handleFeignException_handlesEdgeCases() {
        // No body
        FeignException noBody = new FeignException.BadGateway(
                "gateway down", Request.create(Request.HttpMethod.GET, "/quotes",
                Map.of(), null, StandardCharsets.UTF_8, null), new byte[0], null);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        ApiRespond<Void> fallback = handler.handleFeignException(noBody, resp);
        verify(resp).setStatus(HttpStatus.BAD_GATEWAY.value());
        assertThat(fallback.getMsg()).contains("gateway down");

        // Malformed JSON body
        FeignException badJson = feignExceptionRaw(400, "not-json");
        HttpServletResponse resp2 = mock(HttpServletResponse.class);
        ApiRespond<Void> parsed = handler.handleFeignException(badJson, resp2);
        verify(resp2).setStatus(HttpStatus.BAD_REQUEST.value());
        assertThat(parsed.getMsg()).contains("not-json");
    }

    @DisplayName("handleFeignException maps payment and token errors and defaults unknown codes")
    @Test
    void handleFeignException_otherCodes() {
        FeignException payment = feignException(402, Map.of("error_code", "PAYMENT_FAILED"));
        HttpServletResponse paymentResp = mock(HttpServletResponse.class);
        ApiRespond<Void> paymentResult = handler.handleFeignException(payment, paymentResp);
        verify(paymentResp).setStatus(HttpStatus.PAYMENT_REQUIRED.value());
        assertThat(paymentResult.getMsg()).contains("ERR_PAYMENT_FAILED");

        FeignException invalidToken = feignException(400, Map.of("error_code", "INVALID_QUOTE_TOKEN", "message", "bad token"));
        HttpServletResponse invalidResp = mock(HttpServletResponse.class);
        ApiRespond<Void> invalidResult = handler.handleFeignException(invalidToken, invalidResp);
        verify(invalidResp).setStatus(HttpStatus.BAD_REQUEST.value());
        assertThat(invalidResult.getMsg()).contains("ERR_INVALID_QUOTE_TOKEN").contains("bad token");

        FeignException unknown = feignException(503, Map.of("code", "UNKNOWN", "message", "oops"));
        HttpServletResponse unknownResp = mock(HttpServletResponse.class);
        ApiRespond<Void> unknownResult = handler.handleFeignException(unknown, unknownResp);
        verify(unknownResp).setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(unknownResult.getMsg()).contains("oops");

        FeignException blankBody = feignExceptionRaw(500, "");
        HttpServletResponse blankResp = mock(HttpServletResponse.class);
        ApiRespond<Void> blankResult = handler.handleFeignException(blankBody, blankResp);
        verify(blankResp).setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(blankResult.getMsg()).contains("bookingClient#quote");
    }

    private FeignException feignException(int status, Map<String, String> body) {
        try {
            String json = new ObjectMapper().writeValueAsString(body);
            return feignExceptionRaw(status, json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FeignException feignExceptionRaw(int status, String body) {
        Request request = Request.create(Request.HttpMethod.POST, "/booking/quote", Map.of(), new byte[0],
                StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .request(request)
                .headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("bookingClient#quote", response);
    }
}
