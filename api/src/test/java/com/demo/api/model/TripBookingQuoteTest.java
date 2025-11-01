package com.demo.api.model;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TripBookingQuoteTest {

    @Test
    void onCreate_setsDefaultsWhenMissing() {
        TripBookingQuote quote = TripBookingQuote.builder()
                .tripId(1L)
                .itemReference("hotel_1")
                .productType("hotel")
                .entityId(1L)
                .status("")
                .build();

        quote.onCreate();

        assertThat(quote.getCreatedAt()).isNotNull();
        assertThat(quote.getStatus()).isEqualTo("confirm");
    }

    @Test
    void onCreate_preservesExistingValues() {
        TripBookingQuote quote = TripBookingQuote.builder()
                .tripId(1L)
                .itemReference("hotel_1")
                .productType("hotel")
                .entityId(1L)
                .createdAt(LocalDate.of(2025, 1, 1))
                .status("pending")
                .build();

        quote.onCreate();

        assertThat(quote.getCreatedAt()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(quote.getStatus()).isEqualTo("pending");
    }
}
