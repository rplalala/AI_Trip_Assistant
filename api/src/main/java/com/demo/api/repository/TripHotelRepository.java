package com.demo.api.repository;

import com.demo.api.model.TripHotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TripHotelRepository extends JpaRepository<TripHotel, Long> {

    List<TripHotel> findByTripId(Long tripId);

    void deleteByTripIdIn(Collection<Long> tripIds);

    List<TripHotel> findAllByTripId(Long tripId);
}

