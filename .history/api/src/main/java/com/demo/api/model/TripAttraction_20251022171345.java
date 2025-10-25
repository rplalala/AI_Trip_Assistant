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
 * Attraction or activity recommendation for the trip itinerary.
 */
@Entity
@Table(name = "trip_attraction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripAttraction {

    /** Primary key for the attraction record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifier of the trip preference that the attraction belongs to. */
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private LocalDate date;

    private String time;

    private String title;

    @Builder.Default
    private String status = "pending";

    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    @Column(name = "image_url")
    private String imageUrl;

    private String location;

    @Column(name = "ticket_price")
    private Integer ticketPrice;

    // Number of people covered by the ticket price.
    private Integer people;

    private String currency;
}

