package com.demo.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

/**
 * Supported map providers that can supply routing data to the frontend.
 */
public enum MapProvider {
    GOOGLE("google"),
    AMAP("amap");

    private final String id;

    MapProvider(String id) {
        this.id = id;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @JsonCreator
    public static MapProvider fromJson(String raw) {
        return fromId(raw);
    }

    public static MapProvider fromId(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("map provider cannot be null");
        }
        String normalized = raw.trim().toLowerCase(Locale.ENGLISH);
        return Arrays.stream(values())
                .filter(provider -> provider.id.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown map provider: " + raw));
    }
}
