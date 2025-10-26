package com.demo.externalservice.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

final class PricingSupport {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private PricingSupport() {
    }

    static String stringParam(Map<String, Object> params, String key, String defaultValue) {
        return Optional.ofNullable(params.get(key))
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .orElse(defaultValue);
    }

    static boolean booleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return defaultValue;
    }

    static int intParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    static LocalDate dateParam(Map<String, Object> params, String key, LocalDate defaultValue) {
        String value = stringParam(params, key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static BigDecimal decimalParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(0, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(0, RoundingMode.HALF_UP);
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s).setScale(0, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
