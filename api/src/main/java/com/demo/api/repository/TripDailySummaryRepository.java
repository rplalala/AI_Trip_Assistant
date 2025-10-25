package com.demo.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.api.model.TripDailySummary;

public interface TripDailySummaryRepository extends JpaRepository<TripDailySummary, Long> {

    List<TripDailySummary> findByTripId(Long tripId);
}

