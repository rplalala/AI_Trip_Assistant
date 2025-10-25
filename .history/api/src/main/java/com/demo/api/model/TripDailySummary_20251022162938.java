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

    /** Primary key for the daily summary entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifier of the related trip preference request. */
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    /** Calendar date the summary applies to. */
    @Column(nullable = false)
    private LocalDate date;

    /** Optional time context for the summary (e.g. morning, evening). */
    private String time;

    /** Title rendered for UI cards or itinerary sections. */
    private String title;

    /** Processing state of the generated summary. */
    @Builder.Default
    private String status = "pending";

    /** Flag indicating if a booking must be made. */
    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    /** Optional image URL associated with the summary. */
    @Column(name = "image_url")
    private String imageUrl;

    /** Generated summary text for the day. */
    @Column(length = 4096)
    private String summary;
}
