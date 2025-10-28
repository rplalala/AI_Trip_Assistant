package com.demo.api.repository;

import com.demo.api.model.TripTransportation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TripTransportationRepository extends JpaRepository<TripTransportation, Long> {

    List<TripTransportation> findByTripId(Long tripId);

    void deleteByTripIdIn(Collection<Long> tripIds);

    List<TripTransportation> findAllByTripId(Long tripId);
}

