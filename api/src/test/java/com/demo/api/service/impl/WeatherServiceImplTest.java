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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
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
import static org.assertj.core.api.Assertions.assertThatCode;

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

        assertThatCode(() -> weatherService.fetchAndStoreWeather(trip)).doesNotThrowAnyException();
        verify(tripWeatherRepository, never()).saveAll(any());
        verify(tripWeatherRepository, never()).save(any());
    }

    @Test
    void fetchAndStoreWeather_whenMalformedJson_throwsIllegalState() {
        Trip trip = Trip.builder()
                .id(11L).toCity("Sydney")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1))
                .build();
        when(restTemplate.getForObject(any(), eq(String.class)))
                .thenReturn("{not-json");

        assertThatThrownBy(() -> weatherService.fetchAndStoreWeather(trip))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to parse weather response");
        verify(tripWeatherRepository, never()).saveAll(any());
    }

    @Test
    void fetchAndStoreWeather_whenApiReturnsCod404_doesNotPersist() {
        Trip trip = Trip.builder()
                .id(12L)
                .toCity("Nowhere")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .build();

        when(restTemplate.getForObject(any(), eq(String.class)))
                .thenReturn("{\"cod\":\"404\",\"message\":\"city not found\"}");

        weatherService.fetchAndStoreWeather(trip);

        verify(restTemplate).getForObject(any(), eq(String.class));
        verifyNoInteractions(tripWeatherRepository);
    }

    @Test
    void fetchAndStoreWeather_whenRestTemplateNotFoundException_isTolerated() {
        Trip trip = Trip.builder()
                .id(14L)
                .toCity("Sydney")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build();

        when(restTemplate.getForObject(any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(
                        "Not Found", org.springframework.http.HttpStatus.NOT_FOUND, "Not Found",
                        null, null, null));

        weatherService.fetchAndStoreWeather(trip);

        verify(restTemplate).getForObject(any(), eq(String.class));
        verifyNoInteractions(tripWeatherRepository);
    }

    @Test
    void fetchAndStoreWeather_whenRestClientException_isTolerated() {
        Trip trip = Trip.builder()
                .id(15L)
                .toCity("Sydney")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build();

        when(restTemplate.getForObject(any(), eq(String.class)))
                .thenThrow(new RestClientException("gateway timeout"));

        weatherService.fetchAndStoreWeather(trip);

        verify(restTemplate).getForObject(any(), eq(String.class));
        verifyNoInteractions(tripWeatherRepository);
    }

    @Test
    void fetchAndStoreWeather_whenNoSummariesWithinWindow_skipsSave() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = start.plusDays(1);
        Trip trip = Trip.builder()
                .id(99L)
                .toCity("London")
                .toCountry("GB")
                .startDate(start)
                .endDate(end)
                .build();

        Instant dtPast = start.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dtFuture = end.plusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC);

        String payload = """
                {
                  "city":{"timezone":0},
                  "list":[
                    {
                      "dt":%d,
                      "main":{"temp_min":10.0,"temp_max":18.0},
                      "weather":[{"main":"Rain"}]
                    },
                    {
                      "dt":%d,
                      "main":{"temp_min":12.0,"temp_max":20.0},
                      "weather":[{"main":"Sunny"}]
                    }
                  ]
                }
                """.formatted(dtPast.getEpochSecond(), dtFuture.getEpochSecond());

        when(restTemplate.getForObject(any(), eq(String.class))).thenReturn(payload);
        weatherService.fetchAndStoreWeather(trip);

        verify(tripWeatherRepository, never()).saveAll(any());
    }

    @Test
    void fetchAndStoreWeather_updatesExistingWeatherRecords() {
        LocalDate start = LocalDate.now().plusDays(1);
        Trip trip = Trip.builder()
                .id(101L)
                .toCity("Paris")
                .toCountry("FR")
                .startDate(start)
                .endDate(start.plusDays(1))
                .build();

        Instant dt1 = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dt2 = start.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        String payload = """
                {
                  "city":{"timezone":0},
                  "list":[
                    {
                      "dt":%d,
                      "main":{"temp_min":11.0,"temp_max":19.0},
                      "weather":[{"main":"Clouds"}]
                    },
                    {
                      "dt":%d,
                      "main":{"temp_min":8.0,"temp_max":21.0},
                      "weather":[{"main":"Clear"}]
                    }
                  ]
                }
                """.formatted(dt1.getEpochSecond(), dt2.getEpochSecond());

        TripWeather existing = new TripWeather();
        existing.setTripId(101L);
        existing.setDate(start);
        existing.setMinTemp(5.0);
        existing.setMaxTemp(9.0);
        existing.setWeatherCondition("Snow");

        when(restTemplate.getForObject(any(), eq(String.class))).thenReturn(payload);
        when(tripWeatherRepository.findByTripId(101L)).thenReturn(List.of(existing));

        weatherService.fetchAndStoreWeather(trip);

        ArgumentCaptor<List<TripWeather>> captor = ArgumentCaptor.forClass(List.class);
        verify(tripWeatherRepository).saveAll(captor.capture());
        List<TripWeather> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        TripWeather updated = saved.stream()
                .filter(w -> w.getDate().equals(start))
                .findFirst()
                .orElseThrow();
        assertThat(updated.getMinTemp()).isEqualTo(11.0);
        assertThat(updated.getMaxTemp()).isEqualTo(19.0);
        assertThat(updated.getWeatherCondition()).isEqualTo("Clouds");
    }

}
