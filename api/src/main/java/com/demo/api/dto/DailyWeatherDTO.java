package com.demo.api.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Daily weather summary returned from OpenWeatherMap processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyWeatherDTO {

    /** Forecast date in destination local time. */
    private LocalDate date;

    /** Minimum temperature measured for the day (metric units). */
    private Double minTemp;

    /** Maximum temperature measured for the day (metric units). */
    private Double maxTemp;

    /** Most frequent weather condition across the day. */
    private String weatherCondition;
}

