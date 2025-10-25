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
 * This service communicates with the OpenWeatherMap API to fetch
 * the 5-day/3-hour forecast, summarize daily weather information,
 * and store it into the database for a given trip.
 *
 * Idempotency: {@link #fetchAndStoreWeather(TripPreference)} now upserts records per (tripId, date)
 * so repeated calls for the same trip do not create duplicate rows.
 * Consider reinforcing this in the database with a UNIQUE constraint on (trip_id, date).
 *
 * ⚠️ Note: Only supports forecast for trips starting within 5 days from today.
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

    /**
     * Main entry point for this service.
     *
     * 1. Validates input trip preference
     * 2. Builds forecast API URL
     * 3. Calls OpenWeatherMap to fetch 5-day forecast
     * 4. Groups 3-hour entries by day
     * 5. Summarizes daily weather (min/max temperature and condition)
     * 6. Saves summarized results into TripWeather table
     */
    @Override
    @Transactional
    public void fetchAndStoreWeather(TripPreference preference) {
        validatePreference(preference);  // Check tripId and city presence
        LocalDate start = preference.getStartDate();
        LocalDate end = Optional.ofNullable(preference.getEndDate()).orElse(start);
        log.debug("Starting weather fetch for trip {} between {} and {}", preference.getTripId(), start, end);

        // 1. Build API URL based on trip city and country
        URI uri = buildForecastUri(preference);

        // 2. Call the OpenWeatherMap API via RestTemplate
        String responseJson = restTemplate.getForObject(uri, String.class);
        if (!StringUtils.hasText(responseJson)) {
            throw new IllegalStateException("Empty response received from OpenWeatherMap");
        }
        log.trace("OpenWeatherMap raw response: {}", responseJson);

        // 3. Parse JSON response into structured objects
        OpenWeatherForecastResponse forecastResponse = parseResponse(responseJson);
        if (forecastResponse == null || forecastResponse.getList() == null || forecastResponse.getList().isEmpty()) {
            log.warn("No forecast data returned for trip {}", preference.getTripId());
            return;
        }

        List<ForecastEntry> forecastEntries = forecastResponse.getList().stream()
                .filter(Objects::nonNull)
                .toList();
        log.debug("OpenWeatherMap returned {} forecast entries for trip {}", forecastEntries.size(), preference.getTripId());

        // Extract timezone offset from response (in seconds)
        Integer timezoneOffsetSeconds = Optional.ofNullable(forecastResponse.getCity())
                .map(City::getTimezone)
                .orElse(0);

        // 4. Filter to the requested trip window and group 3-hour forecast entries by date (local time)
        List<Map.Entry<LocalDate, ForecastEntry>> windowedEntries = forecastEntries.stream()
                .map(entry -> Map.entry(toLocalDate(entry, timezoneOffsetSeconds), entry))
                .filter(pair -> !pair.getKey().isBefore(start) && !pair.getKey().isAfter(end))
                .toList();

        log.debug("Processing {} forecast entries within date window {} - {} for trip {}",
                windowedEntries.size(), start, end, preference.getTripId());

        if (windowedEntries.isEmpty()) {
            log.info("No forecast entries fall within trip window {} - {} for trip {}", start, end, preference.getTripId());
            return;
        }

        Map<LocalDate, List<ForecastEntry>> groupedByDate = windowedEntries.stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        // 5. Generate daily summaries (min/max temp, dominant condition)
        List<DailyWeatherDTO> dailySummaries = groupedByDate.entrySet().stream()
                .map(entry -> buildDailySummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailyWeatherDTO::getDate))
                .toList();
        log.debug("Aggregated {} daily weather summaries for trip {}", dailySummaries.size(), preference.getTripId());

        // 6. Upsert daily summaries to avoid duplicate rows when the method is invoked multiple times.
        dailySummaries.forEach(summary -> {
            LocalDate date = summary.getDate();
            boolean exists = tripWeatherRepository.existsByTripIdAndDate(preference.getTripId(), date);

            if (exists) {
                TripWeather existing = tripWeatherRepository.findByTripIdAndDate(preference.getTripId(), date)
                        .orElse(null);

                if (existing == null) {
                    log.warn("Expected weather record for trip {} on {} but none found during update. Inserting new row.",
                            preference.getTripId(), date);
                    TripWeather weather = modelMapper.map(summary, TripWeather.class);
                    weather.setTripId(preference.getTripId());
                    TripWeather saved = tripWeatherRepository.save(weather);
                    log.debug("Saved new weather record {} for trip {} on {} after missing expected row",
                            saved.getId(), preference.getTripId(), date);
                    return;
                }

                existing.setDate(summary.getDate());
                existing.setMinTemp(summary.getMinTemp());
                existing.setMaxTemp(summary.getMaxTemp());
                existing.setWeatherCondition(summary.getWeatherCondition());
                TripWeather saved = tripWeatherRepository.save(existing);
                log.debug("Updated weather record {} for trip {} on {}", saved.getId(), preference.getTripId(), date);
            } else {
                TripWeather weather = modelMapper.map(summary, TripWeather.class);
                weather.setTripId(preference.getTripId());
                TripWeather saved = tripWeatherRepository.save(weather);
                log.debug("Saved new weather record {} for trip {} on {}", saved.getId(), preference.getTripId(), date);
            }
        });

        log.info("Upserted {} daily weather records for trip {}", dailySummaries.size(), preference.getTripId());
        // TODO: enforce UNIQUE (trip_id, date) at the database layer for additional safety.
    }

    // ---------------- Helper Methods ---------------- //

    /**
     * Validates the trip preference input before API call.
     */
    private void validatePreference(TripPreference preference) {
        if (preference == null) {
            throw new IllegalArgumentException("Trip preference is required");
        }
        if (preference.getTripId() == null) {
            throw new IllegalArgumentException("Trip preference must include a tripId");
        }
        if (!StringUtils.hasText(preference.getToCity())) {
            throw new IllegalArgumentException("Trip preference must include a city");
        }
        if (preference.getStartDate() == null || preference.getEndDate() == null) {
            throw new IllegalArgumentException("Trip preference must include start and end dates");
        }
        if (preference.getEndDate().isBefore(preference.getStartDate())) {
            throw new IllegalArgumentException("Trip preference end date must not be before start date");
        }
    }

    /**
     * Constructs the full URI for OpenWeatherMap forecast API.
     */
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

    /**
     * Parses the raw JSON string into a structured response object.
     */
    private OpenWeatherForecastResponse parseResponse(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, OpenWeatherForecastResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse weather response", ex);
        }
    }

    /**
     * Converts the Unix timestamp (dt) into a local date, considering timezone offset.
     */
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

    /**
     * Builds a single-day weather summary (min temp, max temp, main condition)
     * based on multiple 3-hour forecast entries.
     */
    private DailyWeatherDTO buildDailySummary(LocalDate date, List<ForecastEntry> entries) {
        // Compute min and max temperature
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

        // Find the most frequent weather condition (e.g., "Cloudy", "Rain")
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

    // ---------------- JSON Response Mapping Classes ---------------- //

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenWeatherForecastResponse {
        private List<ForecastEntry> list;  // List of forecast items
        private City city;                 // City info (includes timezone)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ForecastEntry {
        private long dt;                   // Forecast timestamp
        private MainData main;             // Temperature info
        private List<WeatherDescription> weather;  // Weather condition list
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MainData {
        @JsonProperty("temp_min")
        private Double tempMin;            // Minimum temperature
        @JsonProperty("temp_max")
        private Double tempMax;            // Maximum temperature
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WeatherDescription {
        private String main;               // Main weather condition (e.g. "Cloudy")
        private String description;        // Detailed description (e.g. "broken clouds")
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class City {
        private String name;
        private String country;
        private Integer timezone;          // Offset in seconds from UTC
    }
}
