package com.demo.externalservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "booking")
public class BookingProperties {

    /**
     * Secret key used to sign quote tokens (HMAC).
     */
    private String tokenSecret = "dev-secret-change-me-please-rotate-2024";

    /**
     * Token time-to-live.
     */
    private Duration tokenTtl = Duration.ofMinutes(15);

    /**
     * Idempotency window.
     */
    private Duration idempotencyTtl = Duration.ofMinutes(30);

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public Duration getIdempotencyTtl() {
        return idempotencyTtl;
    }

    public void setIdempotencyTtl(Duration idempotencyTtl) {
        this.idempotencyTtl = idempotencyTtl;
    }
}
