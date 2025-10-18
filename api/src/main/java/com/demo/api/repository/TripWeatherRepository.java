package com.demo.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.api.model.TripWeather;

public interface TripWeatherRepository extends JpaRepository<TripWeather, Long> {

    List<TripWeather> findByTripId(Long tripId);
}

