package com.demo.api.service.impl;

import com.demo.api.client.OpenAiClient;
import com.demo.api.dto.ItineraryDTO;
import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.dto.ModifyPlanDTO;
import com.demo.api.model.Trip;
import com.demo.api.model.TripWeather;
import com.demo.api.repository.TripRepository;
import com.demo.api.repository.TripWeatherRepository;
import com.demo.api.service.TripStorageService;
import com.demo.api.service.WeatherService;
import com.demo.api.service.prompt.TripPlanPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TripGenerationServiceImplTest {

    @Mock private WeatherService weatherService;
    @Mock private TripWeatherRepository tripWeatherRepository;
    @Mock private TripPlanPromptBuilder tripPlanPromptBuilder;
    @Mock private ObjectProvider<OpenAiClient> openAiClientProvider;
    @Mock private ObjectProvider<TripStorageService> tripStorageServiceProvider;
    @Mock private TripRepository tripRepository;
    @Mock private OpenAiClient openAiClient;
    @Mock private TripStorageService tripStorageService;

    private TripGenerationServiceImpl tripGenerationService;

    @BeforeEach
    void setUp() {
        tripGenerationService = new TripGenerationServiceImpl(
                new ModelMapper(),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                weatherService,
                tripWeatherRepository,
                tripPlanPromptBuilder,
                openAiClientProvider,
                tripStorageServiceProvider,
                tripRepository
        );
    }

    @Test
    void generateTripAndReturnJson_triggersWeatherFetchAndStorage() {
        TripPreferenceRequestDTO dto = TripPreferenceRequestDTO.builder()
                .fromCity("Sydney")
                .toCity("Tokyo")
                .currency("AUD")
                .people(2)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip entity = invocation.getArgument(0);
            entity.setId(321L);
            return entity;
        });
        TripWeather weather = TripWeather.builder()
                .tripId(321L)
                .date(LocalDate.now().plusDays(1))
                .minTemp(10.0)
                .maxTemp(18.0)
                .weatherCondition("Cloudy")
                .build();
        when(tripWeatherRepository.findByTripId(321L)).thenReturn(List.of(weather));
        when(tripPlanPromptBuilder.build(any(Trip.class), anyList())).thenReturn("prompt");
        when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
        when(tripStorageServiceProvider.getIfAvailable()).thenReturn(tripStorageService);
        when(openAiClient.requestTripPlan("prompt")).thenReturn("{\"daily_summaries\":[],\"activities\":[]}");
        ItineraryDTO itineraryDTO = ItineraryDTO.builder()
                .dailySummaries(List.of())
                .activities(List.of())
                .build();
        when(openAiClient.parseContent(anyString(), eq(ItineraryDTO.class))).thenReturn(itineraryDTO);

        tripGenerationService.generateTripAndReturnJson(dto, "42");

        verify(weatherService).fetchAndStoreWeather(argThat(trip -> trip.getUserId().equals(42L)));
        verify(openAiClient).requestTripPlan("prompt");
        verify(tripStorageService).storeTripPlan(any(Trip.class), contains("\"daily_summaries\""));
    }

    @Test
    void generateTripAndReturnJson_whenOpenAiClientMissing_throwsIllegalState() {
        TripPreferenceRequestDTO dto = TripPreferenceRequestDTO.builder()
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(4))
                .toCity("Kyoto")
                .build();

        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip entity = invocation.getArgument(0);
            entity.setId(999L);
            return entity;
        });
        when(openAiClientProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> tripGenerationService.generateTripAndReturnJson(dto, "5"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAiClient bean is not configured");
    }

    @Test
    void regenerateTrip_fetchesExistingTripAndStoresPlan() {
        Trip trip = Trip.builder()
                .id(55L)
                .userId(7L)
                .toCity("Seoul")
                .currency("KRW")
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(6))
                .build();
        when(tripRepository.findByIdAndUserId(55L, 7L)).thenReturn(Optional.of(trip));
        when(tripWeatherRepository.findByTripId(55L)).thenReturn(List.of(
                TripWeather.builder()
                        .tripId(55L)
                        .date(LocalDate.now().plusDays(3))
                        .minTemp(12.0)
                        .maxTemp(24.0)
                        .weatherCondition("Clear")
                        .build()
        ));
        when(tripPlanPromptBuilder.buildForRegeneration(eq(trip), anyList(), any(ModifyPlanDTO.class)))
                .thenReturn("regen-prompt");
        when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
        when(tripStorageServiceProvider.getIfAvailable()).thenReturn(tripStorageService);
        when(openAiClient.requestTripPlan("regen-prompt")).thenReturn("{\"daily_summaries\":[],\"activities\":[]}");
        when(openAiClient.parseContent(anyString(), eq(ItineraryDTO.class))).thenReturn(
                ItineraryDTO.builder().dailySummaries(List.of()).activities(List.of()).build()
        );

        tripGenerationService.regenerateTrip(55L, new ModifyPlanDTO(), "7");

        verify(tripWeatherRepository).deleteByTripIdIn(List.of(55L));
        verify(weatherService).fetchAndStoreWeather(trip);
        verify(openAiClient).requestTripPlan("regen-prompt");
        verify(tripStorageService).storeTripPlan(eq(trip), contains("\"activities\""));
    }
}
