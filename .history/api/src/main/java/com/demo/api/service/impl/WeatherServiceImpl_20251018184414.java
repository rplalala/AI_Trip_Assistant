package com.demo.api.service.impl;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.demo.api.dto.DailyWeatherDTO;
import com.demo.api.model.TripPreference;
import com.demo.api.model.TripWeather;
import com.demo.api.repository.TripWeatherRepository;
import com.demo.api.service.WeatherService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Fetches weather data from OpenWeatherMap and stores daily summaries for trips.
 */
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherServiceImpl.class);
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";

    private final RestTemplate restTemplate;
    private final TripWeatherRepository tripWeatherRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    @Value("${openweather.api.key}")
    private String apiKey;

    @Override
    @Transactional
    public void fetchAndStoreWeather(TripPreference preference) {
        validatePreference(preference);

        URI uri = buildForecastUri(preference);
        String responseJson = restTemplate.getForObject(uri, String.class);
        if (!StringUtils.hasText(responseJson)) {
            throw new IllegalStateException("Empty response received from OpenWeatherMap");
        }
        log.debug("OpenWeatherMap raw response: {}", responseJson);

        OpenWeatherForecastResponse forecastResponse = parseResponse(responseJson);
        if (forecastResponse == null || forecastResponse.getList() == null || forecastResponse.getList().isEmpty()) {
            log.warn("No forecast data returned for trip {}", preference.getTripId());
            return;
        }

        Integer timezoneOffsetSeconds = Optional.ofNullable(forecastResponse.getCity())
                .map(City::getTimezone)
                .orElse(0);

        Map<LocalDate, List<ForecastEntry>> groupedByDate = forecastResponse.getList().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(entry -> toLocalDate(entry, timezoneOffsetSeconds)));

        List<DailyWeatherDTO> dailySummaries = groupedByDate.entrySet().stream()
                .map(entry -> buildDailySummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailyWeatherDTO::getDate))
                .toList();

        List<TripWeather> entities = dailySummaries.stream()
                .map(dto -> {
                    TripWeather weather = modelMapper.map(dto, TripWeather.class);
                    weather.setTripId(preference.getTripId());
                    return weather;
                })
                .toList();

        tripWeatherRepository.saveAll(entities);
        log.info("Stored {} daily weather records for trip {}", entities.size(), preference.getTripId());
    }

    private void validatePreference(TripPreference preference) {
        if (preference == null) {
            throw new IllegalArgumentException("Trip preference is required");
        }
        if (preference.getTripId() == null) {
            throw new IllegalArgumentException("Trip preference must include a tripId");
        }
        if (!StringUtils.hasText(preference.getToCity())) {
            throw new IllegalArgumentException("Trip preference must include a destination city");
        }
    }

    private URI buildForecastUri(TripPreference preference) {
        String location = Stream.of(preference.getToCity(), preference.getToCountry())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));

        return UriComponentsBuilder.fromHttpUrl(FORECAST_URL)
                .queryParam("q", location)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .build()
                .encode()
                .toUri();
    }

    private OpenWeatherForecastResponse parseResponse(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, OpenWeatherForecastResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse weather response", ex);
        }
    }

    private LocalDate toLocalDate(ForecastEntry entry, Integer timezoneOffsetSeconds) {
        ZoneOffset zoneOffset = ZoneOffset.UTC;
        if (timezoneOffsetSeconds != null) {
            try {
                zoneOffset = ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds);
            } catch (Exception ignored) {
                // Fallback to UTC if timezone is invalid
            }
        }

        return Instant.ofEpochSecond(entry.getDt())
                .atOffset(zoneOffset)
                .toLocalDate();
    }

    private DailyWeatherDTO buildDailySummary(LocalDate date, List<ForecastEntry> entries) {
        Double minTemp = entries.stream()
                .map(ForecastEntry::getMain)
                .filter(Objects::nonNull)
                .map(MainData::getTempMin)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .orElse(null);

        Double maxTemp = entries.stream()
                .map(ForecastEntry::getMain)
                .filter(Objects::nonNull)
                .map(MainData::getTempMax)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        String dominantCondition = entries.stream()
                .flatMap(entry -> Optional.ofNullable(entry.getWeather()).orElse(List.of()).stream())
                .map(WeatherDescription::getMain)
                .filter(StringUtils::hasText)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(condition -> condition, Collectors.counting()),
                        freq -> freq.entrySet().stream()
                                .max(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                                        .thenComparing(Map.Entry::getKey))
                                .map(Map.Entry::getKey)
                                .orElse(null)
                ));

        return DailyWeatherDTO.builder()
                .date(date)
                .minTemp(minTemp)
                .maxTemp(maxTemp)
                .weatherCondition(dominantCondition)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenWeatherForecastResponse {
        private List<ForecastEntry> list;
        private City city;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ForecastEntry {
        private long dt;
        private MainData main;
        private List<WeatherDescription> weather;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MainData {
        @JsonProperty("temp_min")
        private Double tempMin;
        @JsonProperty("temp_max")
        private Double tempMax;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WeatherDescription {
        private String main;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class City {
        private String name;
        private String country;
        private Integer timezone;
    }
}

