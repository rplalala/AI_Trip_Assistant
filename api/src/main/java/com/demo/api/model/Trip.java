package com.demo.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Trip preference details captured from users to drive trip generation workflows.
 */
@Entity
@Table(name = "trip", indexes = {
        @Index(name = "idx_trip_user_id", columnList = "user_id"),
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip extends BaseModel{

    /** Unique identifier for the saved trip preference record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

