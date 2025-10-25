package com.demo.api.service.impl;

import com.demo.api.model.TripPreference;
import com.demo.api.model.TripWeather;
import com.demo.api.repository.TripWeatherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WeatherServiceImplTest {

    private RestTemplate restTemplate;
    private TripWeatherRepository tripWeatherRepository;
    private WeatherServiceImpl weatherService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        tripWeatherRepository = mock(TripWeatherRepository.class);
        weatherService = new WeatherServiceImpl(restTemplate, tripWeatherRepository, new ModelMapper(), new ObjectMapper());
        ReflectionTestUtils.setField(weatherService, "apiKey", "fake-key");
    }

    @Test
    void fetchAndStoreWeather_filtersToTripWindow_andUpsertsPerDay() {
        TripPreference preference = TripPreference.builder()
                .id(42L)
                .toCity("Sydney")
                .toCountry("AU")
                .startDate(LocalDate.of(2023, 1, 1))
                .endDate(LocalDate.of(2023, 1, 2))
                .build();

        String responseJson = buildForecastResponse();
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(responseJson);

        Map<LocalDate, TripWeather> store = new HashMap<>();

        when(tripWeatherRepository.findByTripId(preference.getId()))
                .thenAnswer(invocation -> store.values().stream().toList());

        when(tripWeatherRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<TripWeather> batch = invocation.getArgument(0, List.class);
                    for (TripWeather weather : batch) {
                        store.put(weather.getDate(), weather);
                    }
                    return batch;
                });

        // First invocation stores new records, second invocation should update in place.
        weatherService.fetchAndStoreWeather(preference);
        weatherService.fetchAndStoreWeather(preference);

        assertEquals(2, store.size(), "Should persist one record per in-range day");

        TripWeather dayOne = store.get(LocalDate.of(2023, 1, 1));
        TripWeather dayTwo = store.get(LocalDate.of(2023, 1, 2));
        assertNotNull(dayOne, "Day one record missing after upsert");
        assertNotNull(dayTwo, "Day two record missing after upsert");

        assertEquals(42L, dayOne.getTripId());
        assertEquals(8.0, dayOne.getMinTemp());
        assertEquals(18.0, dayOne.getMaxTemp());
        assertEquals("Rain", dayOne.getWeatherCondition());

        assertEquals(42L, dayTwo.getTripId());
        assertEquals(20.0, dayTwo.getMinTemp());
        assertEquals(25.0, dayTwo.getMaxTemp());
        assertEquals("Clear", dayTwo.getWeatherCondition());

        assertFalse(store.containsKey(LocalDate.of(2022, 12, 31)));
        assertFalse(store.containsKey(LocalDate.of(2023, 1, 3)));

        verify(tripWeatherRepository, times(2)).saveAll(any());
    }

    private static String buildForecastResponse() {
        long dec31 = epochSeconds(LocalDate.of(2022, 12, 31), 0);
        long jan1Midnight = epochSeconds(LocalDate.of(2023, 1, 1), 0);
        long jan1Morning = epochSeconds(LocalDate.of(2023, 1, 1), 3);
        long jan2Midnight = epochSeconds(LocalDate.of(2023, 1, 2), 0);
        long jan3Midnight = epochSeconds(LocalDate.of(2023, 1, 3), 0);

        return String.format(Locale.ROOT,
                """
                        {
                          "list": [
                            {"dt": %d, "main": {"temp_min": 5, "temp_max": 9}, "weather":[{"main":"Rain"}]},
                            {"dt": %d, "main": {"temp_min": 10, "temp_max": 15}, "weather":[{"main":"Clouds"}]},
                            {"dt": %d, "main": {"temp_min": 8, "temp_max": 18}, "weather":[{"main":"Rain"}]},
                            {"dt": %d, "main": {"temp_min": 20, "temp_max": 25}, "weather":[{"main":"Clear"}]},
                            {"dt": %d, "main": {"temp_min": 30, "temp_max": 35}, "weather":[{"main":"Clear"}]}
                          ],
                          "city": {"timezone": 0}
                        }
                        """,
                dec31, jan1Midnight, jan1Morning, jan2Midnight, jan3Midnight);
    }

    private static long epochSeconds(LocalDate date, int hour) {
        return date.atTime(hour, 0).toEpochSecond(ZoneOffset.UTC);
    }
}
