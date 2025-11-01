package com.demo.api.service.impl;

import com.demo.api.dto.TimeLineDTO;
import com.demo.api.model.Trip;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripDailySummary;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripTransportation;
import com.demo.api.model.TripWeather;
import com.demo.api.repository.*;
import com.demo.api.support.TestDataFactory;
import com.demo.api.utils.UnsplashImgUtils;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TripServiceImplTest {

    @Mock private TripRepository tripRepository;
    @Mock private UnsplashImgUtils unsplashImgUtils;
    @Mock private TripAttractionRepository tripAttractionRepository;
    @Mock private TripHotelRepository tripHotelRepository;
    @Mock private TripTransportationRepository tripTransportationRepository;
    @Mock private TripDailySummaryRepository tripDailySummaryRepository;
    @Mock private TripBookingQuoteRepository tripBookingQuoteRepository;
    @Mock private TripInsightRepository insightRepository;
    @Mock private TripWeatherRepository tripWeatherRepository;

    @InjectMocks
    private TripServiceImpl tripService;

    @Test
    void getTripDetails_enrichesTripsWithImage() {
        Trip trip = TestDataFactory.trip(200L);
        when(tripRepository.findByUserId(101L)).thenReturn(List.of(trip));
        when(unsplashImgUtils.getImgUrls("Tokyo", 1, 600, 400))
                .thenReturn(List.of("https://images/tokyo.jpg"));

        List<?> results = tripService.getTripDetails(101L);

        assertThat(results).hasSize(1);
        assertThat(results).first()
                .hasFieldOrPropertyWithValue("tripId", 200L)
                .hasFieldOrPropertyWithValue("imgUrl", "https://images/tokyo.jpg");
        verify(unsplashImgUtils).getImgUrls("Tokyo", 1, 600, 400);
    }

    @Test
    void deleteTripByIds_whenIdsPresent_clearsAllRepositories() {
        List<Long> ids = List.of(1L, 2L);

        tripService.deleteTripByIds(ids);

        verify(tripWeatherRepository).deleteByTripIdIn(ids);
        verify(insightRepository).deleteByTripIdIn(ids);
        verify(tripBookingQuoteRepository).deleteByTripIdIn(ids);
        verify(tripDailySummaryRepository).deleteByTripIdIn(ids);
        verify(tripTransportationRepository).deleteByTripIdIn(ids);
        verify(tripHotelRepository).deleteByTripIdIn(ids);
        verify(tripAttractionRepository).deleteByTripIdIn(ids);
        verify(tripRepository).deleteAllByIdInBatch(ids);
    }

    @Test
    void getTimeLine_collatesDailySummariesWithActivities() {
        long tripId = 301L;
        LocalDate day1 = LocalDate.of(2025, 4, 1);
        TripDailySummary summary = TestDataFactory.summary(1L, tripId, day1);

        TripAttraction attraction = TestDataFactory.attraction(tripId, day1);
        TripHotel hotel = TestDataFactory.hotel(tripId, day1);
        TripTransportation transport = TestDataFactory.transport(tripId, day1);
        TripWeather weather = TestDataFactory.weather(tripId, day1);

        when(tripDailySummaryRepository.findByTripId(tripId)).thenReturn(List.of(summary));
        when(tripAttractionRepository.findAllByTripId(tripId)).thenReturn(List.of(attraction));
        when(tripHotelRepository.findAllByTripId(tripId)).thenReturn(List.of(hotel));
        when(tripTransportationRepository.findAllByTripId(tripId)).thenReturn(List.of(transport));
        when(tripWeatherRepository.findAllByTripId(tripId)).thenReturn(List.of(weather));

        List<TimeLineDTO> timeline = tripService.getTimeLine(tripId);

        assertThat(timeline).hasSize(1);
        TimeLineDTO dto = timeline.getFirst();
        assertThat(dto.getAttraction()).hasSize(1);
        assertThat(dto.getHotel()).hasSize(1);
        assertThat(dto.getTransportation()).hasSize(1);
        assertThat(dto.getWeatherCondition()).isEqualTo("Sunny");
        assertThat(dto.getDate()).isEqualTo(day1.toString());
    }
}
