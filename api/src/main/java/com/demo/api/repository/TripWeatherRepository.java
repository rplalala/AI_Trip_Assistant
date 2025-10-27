package com.demo.api.repository;

import com.demo.api.model.TripWeather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TripWeatherRepository extends JpaRepository<TripWeather, Long> {

    List<TripWeather> findByTripId(Long tripId);

    void deleteByTripIdIn(Collection<Long> tripIds);
}

