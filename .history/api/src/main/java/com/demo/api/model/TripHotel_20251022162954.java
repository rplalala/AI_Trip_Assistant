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

    /** Primary key for the hotel record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifier of the trip preference that owns the record. */
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    /** Date the stay occurs. */
    @Column(nullable = false)
    private LocalDate date;

    /** Time associated with check-in or activity. */
    private String time;

    /** Title rendered for the itinerary entry. */
    private String title;

    /** Processing lifecycle status for the hotel recommendation. */
    @Builder.Default
    private String status = "pending";

    /** Indicates if reservations must be confirmed. */
    @Column(name = "reservation_required")
    private Boolean reservationRequired;

    /** Optional image illustrating the property. */
    @Column(name = "image_url")
    private String imageUrl;

    /** Name of the hotel. */
    @Column(name = "hotel_name")
    private String hotelName;

    /** Room configuration suggested for the stay. */
    @Column(name = "room_type")
    private String roomType;

    /** Number of guests covered by the booking. */
    private Integer people;

    /** Number of nights reserved. */
    private Integer nights;

    /** Price of the stay. */
    private Integer price;

    /** Currency code for the price. */
    private String currency;
}

