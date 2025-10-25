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
 * Lodging recommendations associated with the trip itinerary.
 */
@Entity
@Table(name = "trip_hotel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripHotel {

    // Primary key for the hotel record.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifier of the trip preference that owns the record.
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private LocalDate date;

    // Time associated with check-in or activity.
    private String time;

    private String title;

    @Builder.Default
    private String status = "pending";

    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "room_type")
    private String roomType;

    private Integer people;

    private Integer nights;

    private Integer price;

    private String currency;
}

