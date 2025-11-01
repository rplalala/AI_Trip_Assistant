package com.demo.api.service.impl;

import com.demo.api.dto.TimeLineDTO;
import com.demo.api.dto.TripDetailDTO;
import com.demo.api.model.*;
import com.demo.api.repository.*;
import com.demo.api.service.TripService;
import com.demo.api.utils.UnsplashImgUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<Trip> userTrips = tripRepository.findByUserIdOrderByUpdatedTimeDesc(userId);
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

    /**
     * Get trip timeline
     * @param tripId
     * @return
     */
    @Override
    public List<TimeLineDTO> getTimeLine(Long tripId) {
        List<TripDailySummary> dailySummaries = tripDailySummaryRepository.findByTripId(tripId);

        // Stream<TripAttraction> -> Map<LocalDate, List<TripAttraction>>
        Map<LocalDate, List<TripAttraction>> attractionsGroupedByDate =
                tripAttractionRepository.findAllByTripId(tripId).stream()
                        .collect(Collectors.groupingBy(TripAttraction::getDate));

        // Stream<TripHotel> -> Map<LocalDate, List<TripHotel>>
        Map<LocalDate, List<TripHotel>> hotelsGroupedByDate =
                tripHotelRepository.findAllByTripId(tripId).stream()
                        .collect(Collectors.groupingBy(TripHotel::getDate));

        // Stream<TripTransportation> -> Map<LocalDate, List<TripTransportation>>
        Map<LocalDate, List<TripTransportation>> transportationsGroupedByDate =
                tripTransportationRepository.findAllByTripId(tripId).stream()
                        .collect(Collectors.groupingBy(TripTransportation::getDate));

        // Stream<TripWeather> -> Map<LocalDate, TripWeather>
        Map<LocalDate, TripWeather> weatherByDateMap =
                tripWeatherRepository.findAllByTripId(tripId).stream()
                        .collect(Collectors.toMap(
                                TripWeather::getDate,
                                tripWeather-> tripWeather
                        ));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        return dailySummaries.stream()
                .sorted(Comparator.comparing(TripDailySummary::getDate))
                .map(summary -> {
                    LocalDate currentDate = summary.getDate();
                    TripWeather weather = weatherByDateMap.get(currentDate);
                    if (weather == null) {
                        log.debug("No weather for tripId={}, date={}", tripId, currentDate);
                    }

                    List<TimeLineDTO.AttractionTimeLineDTO> attractionItems =
                            attractionsGroupedByDate.getOrDefault(currentDate, List.of()).stream()
                                    .map(attraction -> TimeLineDTO.AttractionTimeLineDTO.builder()
                                            .location(attraction.getLocation())
                                            .title(attraction.getTitle())
                                            .time(attraction.getTime())
                                            .build()
                                    ).toList();

                    List<TimeLineDTO.HotelTimeLineDTO> hotelItems =
                            hotelsGroupedByDate.getOrDefault(currentDate, List.of()).stream()
                                    .map(hotel -> TimeLineDTO.HotelTimeLineDTO.builder()
                                            .hotelName(hotel.getHotelName())
                                            .title(hotel.getTitle())
                                            .time(hotel.getTime())
                                            .build()
                                    ).toList();

                    List<TimeLineDTO.TransportationTimeLineDTO> transportationItems =
                            transportationsGroupedByDate.getOrDefault(currentDate, List.of()).stream()
                                    .map(transport -> TimeLineDTO.TransportationTimeLineDTO.builder()
                                            .title(transport.getTitle())
                                            .time(transport.getTime())
                                            .from(transport.getFrom())
                                            .to(transport.getTo())
                                            .build()
                                    ).toList();

                    return TimeLineDTO.builder()
                            .date(currentDate.format(dateFormatter))
                            .summary(summary.getSummary())
                            .imageUrl(summary.getImageUrl())
                            .maxTemperature(weather != null ? weather.getMaxTemp() : null)
                            .minTemperature(weather != null ? weather.getMinTemp() : null)
                            .weatherCondition(weather != null ? weather.getWeatherCondition() : null)
                            .attraction(attractionItems)
                            .hotel(hotelItems)
                            .transportation(transportationItems)
                            .build();
                }).toList();
    }
}
