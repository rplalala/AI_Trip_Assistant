package com.demo.api.repository;

import com.demo.api.model.TripInsight;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TripInsightRepository extends JpaRepository<TripInsight, Long> {

    boolean existsByTripId(Long tripId);

    List<TripInsight> findByTripIdOrderById(@Param("tripId") Long tripId);

    void deleteByTripIdIn(Collection<Long> tripIds);
}
