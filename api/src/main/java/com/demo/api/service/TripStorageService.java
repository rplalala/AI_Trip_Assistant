package com.demo.api.service;

import com.demo.api.model.Trip;

public interface TripStorageService {

    void storeTripPlan(Trip preference, String tripPlanJson);
}

