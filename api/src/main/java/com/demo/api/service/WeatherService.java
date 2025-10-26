package com.demo.api.service;

import com.demo.api.model.Trip;

public interface WeatherService {

    void fetchAndStoreWeather(Trip preference);
}

