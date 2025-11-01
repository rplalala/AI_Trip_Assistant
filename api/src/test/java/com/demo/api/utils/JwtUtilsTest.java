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
}

