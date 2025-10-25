package com.demo.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Daily weather summary persisted for each generated trip.
 */
@Entity
@Table(name = "trip_weather")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripWeather extends BaseModel{

    /** Primary key for the weather record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifier of the related trip preference. */
    private Long tripId;

    /** Calendar date of the weather summary (destination local time). */
    private LocalDate date;

    /** Minimum temperature recorded for the day (metric units). */
    private Double minTemp;

    /** Maximum temperature recorded for the day (metric units). */
    private Double maxTemp;

    /** Dominant weather condition for the day (e.g., "Clouds", "Rain"). */
    private String weatherCondition;
}

