package com.demo.api.repository;

import com.demo.api.model.TripBookingQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripBookingQuoteRepository extends JpaRepository<TripBookingQuote, Long> {

    List<TripBookingQuote> findByTripId(Long tripId);

    Optional<TripBookingQuote> findByTripIdAndEntityId(Long tripId, Long entityId);

    Optional<TripBookingQuote> findByTripIdAndEntityIdAndProductType(Long tripId, Long entityId, String productType);

    void deleteByTripIdIn(Collection<Long> tripIds);
}
