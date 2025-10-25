package com.demo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Trip preference payload received from the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPreferenceRequestDTO {

    /** Country the user will depart from. */
    private String fromCountry;

    /** City the user will depart from. */
    private String fromCity;

    /** Destination country for the journey. */
    private String toCountry;

    /** Destination city for the journey. */
    private String toCity;

    /** Currency code used for pricing and budgeting. */
    private String currency;

    /** Total budget allocated by the user. */
    private Integer budget;

    /** Number of travellers included in the plan. */
    private Integer people;

    /** Requested trip start date. */
    private LocalDate startDate;

    /** Requested trip end date. */
    private LocalDate endDate;

    /** Additional notes or preferences provided by the user. */
    private String preferences;
}

