package com.demo.api.service.impl;

import com.demo.api.dto.ItineraryDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
                tripTransportationRepository,
                tripHotelRepository,
                tripAttractionRepository,
                tripDailySummaryRepository,
                unsplashImgUtils
        );
    }

    @Test
    void storeTripPlan_withStructuredDto_persistsActivitiesAndSummaries() {
        Trip trip = Trip.builder()
                .id(900L)
                .currency("AUD")
                .build();

        when(tripDailySummaryRepository.findByTripId(900L)).thenReturn(List.of());
        when(tripTransportationRepository.findByTripId(900L)).thenReturn(List.of());
        when(tripHotelRepository.findByTripId(900L)).thenReturn(List.of());
        when(tripAttractionRepository.findByTripId(900L)).thenReturn(List.of());

        ItineraryDTO dto = ItineraryDTO.builder()
                .dailySummaries(List.of(
                        ItineraryDTO.DailySummaryDTO.builder()
                                .date(LocalDate.parse("2025-06-01"))
                                .summary("Arrival")
                                .imageUrl("https://img/day1.jpg")
                                .build()
                ))
                .activities(List.of(
                        ItineraryDTO.TransportationDTO.builder()
                                .type("transportation")
                                .date(LocalDate.parse("2025-06-01"))
                                .time("09:00")
                                .title("Flight")
                                .status("planned")
                                .reservationRequired(true)
                                .from("SYD")
                                .to("MEL")
                                .price(320)
                                .currency("AUD")
                                .build(),
                        ItineraryDTO.HotelDTO.builder()
                                .type("hotel")
                                .date(LocalDate.parse("2025-06-01"))
                                .time("15:00")
                                .title("Check in")
                                .status("planned")
                                .hotelName("Harbour Hotel")
                                .price(280)
                                .currency("AUD")
                                .build(),
                        ItineraryDTO.AttractionDTO.builder()
                                .type("attraction")
                                .date(LocalDate.parse("2025-06-01"))
                                .time("19:00")
                                .title("Opera House")
                                .status("planned")
                                .location("Sydney")
                                .ticketPrice(80)
                                .currency("AUD")
                                .build()
                ))
                .build();

        tripStorageService.storeTripPlan(trip, dto);

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
    void storeTripPlan_whenDtoNull_noPersistenceOccurs() {
        Trip trip = Trip.builder().id(1L).build();

        tripStorageService.storeTripPlan(trip, null);

        verifyNoInteractions(tripDailySummaryRepository, tripTransportationRepository,
                tripHotelRepository, tripAttractionRepository);
    }

    @Test
    void storeTripPlan_whenTripIdMissing_throwsIllegalArgument() {
        Trip trip = Trip.builder().build();

        assertThatThrownBy(() -> tripStorageService.storeTripPlan(trip, ItineraryDTO.builder().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tripId");
    }

    @Test
    void storeTripPlan_whenExistingData_cleansThenSavesMappedValues() {
        Trip trip = Trip.builder()
                .id(300L)
                .currency("")
                .build();

        List<TripDailySummary> existingSummaries = List.of(new TripDailySummary());
        List<TripTransportation> existingTransport = List.of(new TripTransportation());
        List<TripHotel> existingHotels = List.of(new TripHotel());
        List<TripAttraction> existingAttractions = List.of(new TripAttraction());

        when(tripDailySummaryRepository.findByTripId(300L)).thenReturn(existingSummaries);
        when(tripTransportationRepository.findByTripId(300L)).thenReturn(existingTransport);
        when(tripHotelRepository.findByTripId(300L)).thenReturn(existingHotels);
        when(tripAttractionRepository.findByTripId(300L)).thenReturn(existingAttractions);

        ItineraryDTO dto = ItineraryDTO.builder()
                .dailySummaries(List.of(
                        ItineraryDTO.DailySummaryDTO.builder()
                                .date(null) // should be skipped
                                .summary("Relax")
                                .imageUrl(null)
                                .build()
                ))
                .activities(List.of(
                        ItineraryDTO.TransportationDTO.builder()
                                .type("transportation")
                                .date(LocalDate.parse("2025-07-01"))
                                .time("08:30")
                                .title("Harbour Ferry")
                                .status(" ") // should default to pending
                                .reservationRequired(false)
                                .from("Circular Quay")
                                .to("Manly")
                                .price(46)
                                .currency("NZD")
                                .imageDescription("sunrise")
                                .build(),
                        ItineraryDTO.HotelDTO.builder()
                                .type("hotel")
                                .date(LocalDate.parse("2025-07-01"))
                                .time("21:15")
                                .title("Harbour Stay")
                                .people(3)
                                .nights(3)
                                .price(199)
                                .build(),
                        ItineraryDTO.AttractionDTO.builder()
                                .type("attraction")
                                .title("Bridge Climb")
                                .ticketPrice(null)
                                .people(null)
                                .build(),
                        ItineraryDTO.ActivityDTO.builder()
                                .type("unknown")
                                .title("Skip me")
                                .build()
                ))
                .build();

        tripStorageService.storeTripPlan(trip, dto);

        verify(tripDailySummaryRepository).deleteAll(existingSummaries);
        verify(tripTransportationRepository).deleteAll(existingTransport);
        verify(tripHotelRepository).deleteAll(existingHotels);
        verify(tripAttractionRepository).deleteAll(existingAttractions);

        verify(tripDailySummaryRepository).saveAll(any());
        verify(tripTransportationRepository).saveAll(any());
        verify(tripHotelRepository).saveAll(any());
        verify(tripAttractionRepository).saveAll(any());

        verifyNoMoreInteractions(tripDailySummaryRepository, tripTransportationRepository,
                tripHotelRepository, tripAttractionRepository);
    }
}
