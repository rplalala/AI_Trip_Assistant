package com.demo.api.repository;

import com.demo.api.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findById(Long tripId);

    List<Trip> findByUserId(Long userId);

    @Query("select t.id from Trip t where t.userId in :userIds")
    List<Long> findIdsByUserIdIn(Collection<Long> userIds);

    @Query("select distinct t.userId from Trip t left join User u on u.id = t.userId where u.id is null")
    List<Long> findRedundantUserIds();

    void deleteByUserIdIn(Collection<Long> userIds);

}
