package com.demo.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.api.model.TripPreference;

public interface TripPreferenceRepository extends JpaRepository<TripPreference, Long> {

    Optional<TripPreference> findByTripId(Long tripId);
}
