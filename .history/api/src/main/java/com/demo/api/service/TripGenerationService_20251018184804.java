package com.demo.api.service;

import com.demo.api.dto.TripPreferenceRequestDTO;

public interface TripGenerationService {

    void generateTripFromPreference(TripPreferenceRequestDTO dto);
}

