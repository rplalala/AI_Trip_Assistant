package com.demo.api.exception;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BookingApiExceptionTest {

    @Test
    void constructor_withHttpStatus_setsFields() {
        BookingApiException ex = new BookingApiException("boom", HttpStatus.BAD_REQUEST, "{\"error\":true}");

        assertThat(ex.getMessage()).contains("boom");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBody()).isEqualTo("{\"error\":true}");
    }

    @Test
    void constructor_withHttpStatusCode_resolvesEnum() {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(502);
        BookingApiException ex = new BookingApiException("fail", statusCode, "body");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getResponseBody()).isEqualTo("body");
    }

    @Test
    void constructor_withNullStatus_defaultsToBadGateway() {
        BookingApiException ex = new BookingApiException("oops", (HttpStatus) null, "msg", null);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getResponseBody()).isEqualTo("msg");
    }

    @Test
    void constructor_withUnresolvableStatus_defaultsToBadGateway() {
        HttpStatusCode custom = HttpStatusCode.valueOf(499);
        BookingApiException ex = new BookingApiException("custom", custom, "resp");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getResponseBody()).isEqualTo("resp");
    }
}
