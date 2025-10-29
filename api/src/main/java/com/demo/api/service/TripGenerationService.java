package com.demo.api.service;

import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.dto.ModifyPlanDTO;

public interface TripGenerationService {

    /**
     * Generates a trip plan based on the provided user preferences.
     *
     * @param dto user-submitted trip preference payload
     */
    // void generateTripFromPreference(TripPreferenceRequestDTO dto);

    void generateTripAndReturnJson(TripPreferenceRequestDTO dto, String userId); // for testing purposes !!!

    void regenerateTrip(Long tripId, ModifyPlanDTO modifyPlanDTO, String userId);
}