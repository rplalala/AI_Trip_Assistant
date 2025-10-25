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

    /** Date the attraction is planned. */
    @Column(nullable = false)
    private LocalDate date;

    /** Time slot for the attraction. */
    private String time;

    /** Title for presentation within the itinerary. */
    private String title;

    /** Current status in the attraction lifecycle. */
    @Builder.Default
    private String status = "pending";

    /** Whether a pre-booking is necessary. */
    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    /** Optional illustrative image URL. */
    @Column(name = "image_url")
    private String imageUrl;

    /** Attraction location or venue name. */
    private String location;

    /** Ticket price for the attraction. */
    @Column(name = "ticket_price")
    private Integer ticketPrice;

    /** Number of people covered by the ticket price. */
    private Integer people;

    /** Currency code for the ticket price. */
    private String currency;
}

