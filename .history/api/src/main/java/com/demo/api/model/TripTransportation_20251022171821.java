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
 * Transportation details produced for each day of the trip plan.
 */
@Entity
@Table(name = "trip_transportation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripTransportation {

    // Primary key for the transportation record.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifier of the originating trip preference.
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private LocalDate date;

    // Time of departure or activity.
    private String time;

    private String title;

    @Builder.Default
    private String status = "pending";

    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "from_location")
    private String from;

    @Column(name = "to_location")
    private String to;

    // Provider of the transportation (e.g., airline)
    private String provider;

    private String ticketType;

    private Integer price;

    private String currency;
}

