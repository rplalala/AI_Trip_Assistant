package com.demo.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.api.model.TripHotel;

public interface TripHotelRepository extends JpaRepository<TripHotel, Long> {

    List<TripHotel> findByTripId(Long tripId);
}

