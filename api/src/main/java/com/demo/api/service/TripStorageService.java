package com.demo.api.service;

import com.demo.api.dto.ItineraryDTO;
import com.demo.api.model.Trip;

public interface TripStorageService {

    void storeTripPlan(Trip preference, ItineraryDTO itinerary);
}

