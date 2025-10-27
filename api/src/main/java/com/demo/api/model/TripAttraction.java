package com.demo.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Attraction or activity recommendation for the trip itinerary.
 */
@Entity
@Table(name = "trip_attraction", indexes = {
        @Index(name = "idx_trip_attraction_trip_id", columnList = "trip_id"),
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripAttraction extends BaseModel{

    // Primary key for the attraction record.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifier of the trip preference that the attraction belongs to.
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

    @Column(name = "image_description")
    private String imageDescription;

    private String location;

    @Column(name = "ticket_price")
    private Integer ticketPrice;

    // Number of people covered by the ticket price.
    private Integer people;

    private String currency;
}

