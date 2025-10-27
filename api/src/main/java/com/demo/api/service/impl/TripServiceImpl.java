package com.demo.api.service.impl;

import com.demo.api.dto.TripDetailDTO;
import com.demo.api.model.Trip;
import com.demo.api.repository.*;
import com.demo.api.service.TripService;
import com.demo.api.utils.UnsplashImgUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripServiceImpl implements TripService {
    private final TripRepository tripRepository;
    private final UnsplashImgUtils unsplashImgUtils;
    private final TripAttractionRepository tripAttractionRepository;
    private final TripHotelRepository tripHotelRepository;
    private final TripTransportationRepository tripTransportationRepository;
    private final TripDailySummaryRepository tripDailySummaryRepository;
    private final TripBookingQuoteRepository tripBookingQuoteRepository;
    private final TripInsightRepository insightRepository;
    private final TripWeatherRepository tripWeatherRepository;

    @Override
    public List<TripDetailDTO> getTripDetails(Long userId) {
        List<Trip> userTrips = tripRepository.findByUserId(userId);
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

    /**
     * Delete trips in batch
     * @param tripIds
     * @return
     */
    @Override
    @Transactional
    public void deleteTripByIds(List<Long> tripIds) {
        log.info("Deleting trips with IDs: {}", tripIds);
        if (!tripIds.isEmpty()) {
            tripWeatherRepository.deleteByTripIdIn(tripIds);
            insightRepository.deleteByTripIdIn(tripIds);
            tripBookingQuoteRepository.deleteByTripIdIn(tripIds);
            tripDailySummaryRepository.deleteByTripIdIn(tripIds);
            tripTransportationRepository.deleteByTripIdIn(tripIds);
            tripHotelRepository.deleteByTripIdIn(tripIds);
            tripAttractionRepository.deleteByTripIdIn(tripIds);
            tripRepository.deleteAllByIdInBatch(tripIds);
        } else{
            log.info("No trips to delete");
        }
    }
}
