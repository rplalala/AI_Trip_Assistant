package com.demo.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.api.model.TripBookingQuote;

public interface TripBookingQuoteRepository extends JpaRepository<TripBookingQuote, Long> {

    List<TripBookingQuote> findByTripId(Long tripId);

    Optional<TripBookingQuote> findByTripIdAndEntityId(Long tripId, Long entityId);

    Optional<TripBookingQuote> findByTripIdAndEntityIdAndProductType(Long tripId, Long entityId, String productType);

}
