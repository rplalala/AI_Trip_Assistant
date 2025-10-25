package com.demo.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.api.model.TripTransportation;

public interface TripTransportationRepository extends JpaRepository<TripTransportation, Long> {

    List<TripTransportation> findByTripId(Long tripId);
}

