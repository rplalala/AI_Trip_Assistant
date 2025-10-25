package com.demo.api.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Daily overview for generated trip itineraries.
 */
@Entity
@Table(name = "trip_daily_summary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDailySummary {

    // Primary key for the daily summary entry.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifier of the related trip preference request.
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(length = 4096)
    private String summary;
}
