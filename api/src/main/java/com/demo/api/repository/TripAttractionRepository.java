package com.demo.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.api.model.TripAttraction;

public interface TripAttractionRepository extends JpaRepository<TripAttraction, Long> {

    List<TripAttraction> findByTripId(Long tripId);
}

