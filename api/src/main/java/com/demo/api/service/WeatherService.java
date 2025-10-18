package com.demo.api.service;

import com.demo.api.model.TripPreference;

public interface WeatherService {

    void fetchAndStoreWeather(TripPreference preference);
}

