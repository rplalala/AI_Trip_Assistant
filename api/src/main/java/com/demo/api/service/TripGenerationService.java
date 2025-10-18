package com.demo.api.service;

import com.demo.api.dto.TripPreferenceRequestDTO;

public interface TripGenerationService {

    /**
     * Generates a trip plan based on the provided user preferences.
     *
     * @param dto user-submitted trip preference payload
     */
    // void generateTripFromPreference(TripPreferenceRequestDTO dto);

    String generateTripAndReturnJson(TripPreferenceRequestDTO dto); // for testing purposes !!!
}