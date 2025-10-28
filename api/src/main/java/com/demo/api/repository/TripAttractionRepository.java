package com.demo.api.repository;

import com.demo.api.model.TripAttraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TripAttractionRepository extends JpaRepository<TripAttraction, Long> {

    List<TripAttraction> findByTripId(Long tripId);

    void deleteByTripIdIn(Collection<Long> tripIds);

    List<TripAttraction> findAllByTripId(Long tripId);
}

