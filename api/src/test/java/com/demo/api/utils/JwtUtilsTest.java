package com.demo.api.utils;

import com.demo.api.exception.BusinessException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JwtUtilsTest {

    private final JwtUtils jwtUtils = new JwtUtils(
            "@#^&*(*&$%@!~~!ELEC5620_AI_Trip_Assistant_@#$%^&*()_+1234567890",
            Duration.ofHours(1).toMillis()
    );

    @Test
    void generateJwt_andParseRoundTrip() {
        String token = jwtUtils.generateJwt("42", Map.of("username", "demo"));

        Claims claims = jwtUtils.parse(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("username", String.class)).isEqualTo("demo");
    }

    @Test
    void parse_whenTokenMalformed_throwsBusinessException() {
        assertThatThrownBy(() -> jwtUtils.parse("not-a-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void parse_whenTokenNearlyExpired_triggersWarningPath() {
        JwtUtils shortLived = new JwtUtils(
                "@#^&*(*&$%@!~~!ELEC5620_AI_Trip_Assistant_@#$%^&*()_+1234567890",
                Duration.ofMinutes(5).toMillis()
        );

        String token = shortLived.generateJwt("100", Map.of());

        Claims claims = shortLived.parse(token);

        assertThat(claims.getSubject()).isEqualTo("100");
    }

    @Test
    void parse_whenTokenExpired_throwsBusinessException() {
        JwtUtils expired = new JwtUtils(
                "@#^&*(*&$%@!~~!ELEC5620_AI_Trip_Assistant_@#$%^&*()_+1234567890",
                -Duration.ofSeconds(1).toMillis()
        );
        String token = expired.generateJwt("200", Map.of());

        assertThatThrownBy(() -> expired.parse(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token has expired");
    }

    @Test
    void parse_whenTokenNull_throwsBusinessException() {
        assertThatThrownBy(() -> jwtUtils.parse(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token is null");
    }

    @Test
    void generateJwt_allowsNullClaims() {
        String token = jwtUtils.generateJwt("55", null);

        Claims claims = jwtUtils.parse(token);

        assertThat(claims.getSubject()).isEqualTo("55");
    }
}

