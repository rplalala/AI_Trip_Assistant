package com.demo.api.service.impl;

import com.demo.api.model.Trip;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripDailySummary;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripTransportation;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripDailySummaryRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.demo.api.utils.UnsplashImgUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TripStorageServiceImplTest {

    private TripStorageServiceImpl tripStorageService;

    @Mock private TripTransportationRepository tripTransportationRepository;
    @Mock private TripHotelRepository tripHotelRepository;
    @Mock private TripAttractionRepository tripAttractionRepository;
    @Mock private TripDailySummaryRepository tripDailySummaryRepository;
    @Mock private UnsplashImgUtils unsplashImgUtils;

    @BeforeEach
    void setUp() {
        tripStorageService = new TripStorageServiceImpl(
                new ObjectMapper(),
                tripTransportationRepository,
                tripHotelRepository,
                tripAttractionRepository,
                tripDailySummaryRepository,
                unsplashImgUtils
        );
    }

    @Test
    void storeTripPlan_withStructuredJson_persistsActivitiesAndSummaries() {
        Trip trip = Trip.builder()
                .id(900L)
                .currency("AUD")
                .build();

        when(tripDailySummaryRepository.findByTripId(900L)).thenReturn(List.of());
        when(tripTransportationRepository.findByTripId(900L)).thenReturn(List.of());
        when(tripHotelRepository.findByTripId(900L)).thenReturn(List.of());
        when(tripAttractionRepository.findByTripId(900L)).thenReturn(List.of());

        String payload = """
                {
                  "daily_summaries": [
                    {"date":"2025-06-01","summary":"Arrival","image_url":"https://img/day1.jpg"}
                  ],
                  "activities": [
                    {
                      "type":"transportation",
                      "date":"2025-06-01",
                      "time":"09:00",
                      "title":"Flight",
                      "status":"planned",
                      "reservation_required":true,
                      "from":"SYD",
                      "to":"MEL",
                      "price":320,
                      "currency":"AUD"
                    },
                    {
                      "type":"hotel",
                      "date":"2025-06-01",
                      "time":"15:00",
                      "title":"Check in",
                      "status":"planned",
                      "hotel_name":"Harbour Hotel",
                      "price":280,
                      "currency":"AUD"
                    },
                    {
                      "type":"attraction",
                      "date":"2025-06-01",
                      "time":"19:00",
                      "title":"Opera House",
                      "status":"planned",
                      "location":"Sydney",
                      "ticket_price":80,
                      "currency":"AUD"
                    }
                  ]
                }
                """;

        tripStorageService.storeTripPlan(trip, payload);

        ArgumentCaptor<List<TripDailySummary>> summariesCaptor = ArgumentCaptor.forClass(List.class);
        verify(tripDailySummaryRepository).saveAll(summariesCaptor.capture());
        TripDailySummary summary = summariesCaptor.getValue().getFirst();
        assertThat(summary.getTripId()).isEqualTo(900L);
        assertThat(summary.getSummary()).isEqualTo("Arrival");

        ArgumentCaptor<List<TripTransportation>> transportCaptor = ArgumentCaptor.forClass(List.class);
        verify(tripTransportationRepository).saveAll(transportCaptor.capture());
        TripTransportation transport = transportCaptor.getValue().getFirst();
        assertThat(transport.getTripId()).isEqualTo(900L);
        assertThat(transport.getFrom()).isEqualTo("SYD");

        ArgumentCaptor<List<TripHotel>> hotelCaptor = ArgumentCaptor.forClass(List.class);
        verify(tripHotelRepository).saveAll(hotelCaptor.capture());
        TripHotel hotel = hotelCaptor.getValue().getFirst();
        assertThat(hotel.getHotelName()).isEqualTo("Harbour Hotel");

        ArgumentCaptor<List<TripAttraction>> attractionCaptor = ArgumentCaptor.forClass(List.class);
        verify(tripAttractionRepository).saveAll(attractionCaptor.capture());
        TripAttraction attraction = attractionCaptor.getValue().getFirst();
        assertThat(attraction.getLocation()).isEqualTo("Sydney");
    }

    @Test
    void storeTripPlan_whenJsonBlank_noPersistenceOccurs() {
        Trip trip = Trip.builder().id(1L).build();

        tripStorageService.storeTripPlan(trip, "   ");

        verifyNoInteractions(tripDailySummaryRepository, tripTransportationRepository,
                tripHotelRepository, tripAttractionRepository);
    }

    @Test
    void storeTripPlan_whenJsonInvalid_wrapsException() {
        Trip trip = Trip.builder().id(2L).build();

        assertThatThrownBy(() -> tripStorageService.storeTripPlan(trip, "{not-json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to persist trip plan JSON");
    }
}
