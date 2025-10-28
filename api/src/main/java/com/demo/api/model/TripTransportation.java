package com.demo.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Transportation details produced for each day of the trip plan.
 */
@Entity
@Table(name = "trip_transportation", indexes = {
        @Index(name = "idx_trip_transportation_trip_id", columnList = "trip_id"),
        @Index(name = "idx_trip_transportation_trip_id_date", columnList = "trip_id,date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripTransportation extends BaseModel{

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

    @Column(name = "image_description")
    private String imageDescription;

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

