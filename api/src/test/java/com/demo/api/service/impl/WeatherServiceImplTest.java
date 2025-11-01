package com.demo.api.service.impl;

import com.demo.api.model.Trip;
import com.demo.api.model.TripWeather;
import com.demo.api.repository.TripWeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WeatherServiceImplTest {

    @Mock private RestTemplate restTemplate;
    @Mock private TripWeatherRepository tripWeatherRepository;

    private WeatherServiceImpl weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherServiceImpl(
                restTemplate,
                tripWeatherRepository,
                new ModelMapper(),
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        ReflectionTestUtils.setField(weatherService, "apiKey", "weather-key");
    }

    @Test
    void fetchAndStoreWeather_withForecastData_persistsDailySummaries() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(1);
        Instant dt1 = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dt2 = start.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        String payload = """
                {
                  "city":{"timezone":0},
                  "list":[
                    {
                      "dt":%d,
                      "main":{"temp_min":11.3,"temp_max":16.7},
                      "weather":[{"main":"Clouds"}]
                    },
                    {
                      "dt":%d,
                      "main":{"temp_min":9.0,"temp_max":18.5},
                      "weather":[{"main":"Clear"}]
                    }
                  ]
                }
                """.formatted(dt1.getEpochSecond(), dt2.getEpochSecond());

        Trip trip = Trip.builder()
                .id(100L)
                .toCity("Tokyo")
                .toCountry("JP")
                .startDate(start)
                .endDate(end)
                .build();

        when(restTemplate.getForObject(any(), eq(String.class))).thenReturn(payload);
        when(tripWeatherRepository.findByTripId(100L)).thenReturn(List.of());

        weatherService.fetchAndStoreWeather(trip);

        ArgumentCaptor<List<TripWeather>> captor = ArgumentCaptor.forClass(List.class);
        verify(tripWeatherRepository).saveAll(captor.capture());
        List<TripWeather> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.getFirst().getWeatherCondition()).isEqualTo("Clouds");
        assertThat(saved.get(1).getMaxTemp()).isEqualTo(18.5);
    }

    @Test
    void fetchAndStoreWeather_whenPreferenceMissingId_throwsIllegalArgument() {
        Trip trip = Trip.builder()
                .toCity("Sydney")
                .startDate(LocalDate.now())
                .build();

        assertThatThrownBy(() -> weatherService.fetchAndStoreWeather(trip))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tripId");
    }

    @Test
    void fetchAndStoreWeather_whenResponseEmpty_noSave() {
        Trip trip = Trip.builder()
                .id(10L)
                .toCity("Sydney")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build();
        when(restTemplate.getForObject(any(), eq(String.class))).thenReturn("");

        assertThatThrownBy(() -> weatherService.fetchAndStoreWeather(trip))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty response");
        verify(tripWeatherRepository, never()).saveAll(any());
    }
}
