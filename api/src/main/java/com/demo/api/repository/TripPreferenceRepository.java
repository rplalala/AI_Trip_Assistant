package com.demo.api.repository;

import com.demo.api.model.TripPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripPreferenceRepository extends JpaRepository<TripPreference, Long> {

    Optional<TripPreference> findById(Long tripId);
}
