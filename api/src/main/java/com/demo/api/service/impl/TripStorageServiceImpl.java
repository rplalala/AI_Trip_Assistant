package com.demo.api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.demo.api.model.TripPreference;
import com.demo.api.service.TripStorageService;

/**
 * Stub storage service that should be replaced with persistent implementation.
 */
@Service
public class TripStorageServiceImpl implements TripStorageService {

    private static final Logger log = LoggerFactory.getLogger(TripStorageServiceImpl.class);

    @Override
    public void storeTripPlan(TripPreference preference, String tripPlanJson) {
        log.warn("TripStorageServiceImpl invoked with preference {}. Replace with persistent storage.", preference);
        log.debug("Trip plan JSON: {}", tripPlanJson);
    }
}

