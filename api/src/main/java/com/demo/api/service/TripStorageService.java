package com.demo.api.service;

import com.demo.api.model.TripPreference;

public interface TripStorageService {

    void storeTripPlan(TripPreference preference, String tripPlanJson);
}

