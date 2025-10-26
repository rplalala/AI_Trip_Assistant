package com.demo.api.repository;

import com.demo.api.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findById(Long tripId);

    List<Trip> findByUserId(Long userId);
}
