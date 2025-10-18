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
 * Trip preference details captured from users to drive trip generation workflows.
 */
@Entity
@Table(name = "trip_preference")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPreference {

    /** Unique identifier for the saved trip preference record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripId;

    /** Identifier of the user who created this preference. */
    private Long userId;

    /** Departure country specified by the user. */
    @Column(name = "from_country")
    private String fromCountry;

    /** Departure city specified by the user. */
    @Column(name = "from_city")
    private String fromCity;

    /** Destination country requested by the user. */
    @Column(name = "to_country")
    private String toCountry;

    /** Destination city requested by the user. */
    @Column(name = "to_city")
    private String toCity;

    /** Currency code used for budgeting. */
    private String currency;

    /** Total budget available for the trip. */
    private Integer budget;

    /** Number of travellers participating in the trip. */
    private Integer people;

    /** Requested trip start date. */
    private LocalDate startDate;

    /** Requested trip end date. */
    private LocalDate endDate;

    /** Additional free-form notes or preferences from the user. */
    @Column(length = 1024)
    private String preferences;
}

