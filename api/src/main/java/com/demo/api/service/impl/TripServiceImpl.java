package com.demo.api.service.impl;

import com.demo.api.dto.TripDetailDTO;
import com.demo.api.model.Trip;
import com.demo.api.repository.TripPreferenceRepository;
import com.demo.api.service.TripService;
import com.demo.api.utils.UnsplashImgUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripServiceImpl implements TripService {
    private final TripPreferenceRepository tripPreferenceRepository;
    private final UnsplashImgUtils unsplashImgUtils;

    @Override
    public List<TripDetailDTO> getTripDetails(Long userId) {
        List<Trip> userTrips = tripPreferenceRepository.findByUserId(userId);
        return userTrips.stream().map(trip -> {
            return TripDetailDTO.builder()
                    .tripId(trip.getId())
                    .fromCountry(trip.getFromCountry())
                    .fromCity(trip.getFromCity())
                    .toCountry(trip.getToCountry())
                    .toCity(trip.getToCity())
                    .budget(trip.getBudget())
                    .people(trip.getPeople())
                    .startDate(trip.getStartDate())
                    .endDate(trip.getEndDate())
                    .imgUrl(unsplashImgUtils.getImgUrls(trip.getToCity(),1, 600,400).getFirst())
                    .build();
        }).toList();

    }
}
