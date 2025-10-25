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

    /** Primary key for the transportation record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifier of the originating trip preference. */
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    /** Date the transportation option applies to. */
    @Column(nullable = false)
    private LocalDate date;

    /** Time of departure or activity. */
    private String time;

    /** Display title for the transportation entry. */
    private String title;

    /** Current processing status for the transportation task. */
    @Builder.Default
    private String status = "pending";

    /** Indicates whether reservations or bookings are required. */
    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    /** Optional image or thumbnail illustrating the transport. */
    @Column(name = "image_url")
    private String imageUrl;

    /** Origin location. */
    @Column(name = "from_location")
    private String from;

    /** Destination location. */
    @Column(name = "to_location")
    private String to;

    /** Provider of the transportation (e.g., airline). */
    private String provider;

    /** Ticket class or type (e.g., economy, first-class). */
    private String ticketType;

    /** Price for the transportation option. */
    private Integer price;

    /** Currency code for the price. */
    private String currency;
}

