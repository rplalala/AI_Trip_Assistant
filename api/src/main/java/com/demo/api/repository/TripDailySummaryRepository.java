package com.demo.api.repository;

import com.demo.api.model.TripDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TripDailySummaryRepository extends JpaRepository<TripDailySummary, Long> {

    List<TripDailySummary> findByTripId(Long tripId);

    void deleteByTripIdIn(Collection<Long> tripIds);
}

