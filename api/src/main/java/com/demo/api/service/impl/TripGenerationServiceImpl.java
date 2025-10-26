package com.demo.api.service.impl;

import com.demo.api.client.OpenAiClient;
import com.demo.api.dto.DailyWeatherDTO;
import com.demo.api.dto.ItineraryDTO;
import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.model.Trip;
import com.demo.api.model.TripWeather;
import com.demo.api.repository.TripRepository;
import com.demo.api.repository.TripWeatherRepository;
import com.demo.api.service.TripGenerationService;
import com.demo.api.service.TripStorageService;
import com.demo.api.service.WeatherService;
import com.demo.api.service.prompt.TripPlanPromptBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the TripGenerationService interface.
 * Handles the complete flow of generating a trip plan:
 * 1. Converting user input (DTO) into entity
 * 2. Optionally fetching weather data
 * 3. Building GPT prompt
 * 4. Calling OpenAI to generate a plan
 * 5. Storing the plan into database
 */
@Service
@RequiredArgsConstructor
public class TripGenerationServiceImpl implements TripGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TripGenerationServiceImpl.class);

    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final WeatherService weatherService;
    private final TripWeatherRepository tripWeatherRepository;
    private final TripPlanPromptBuilder tripPlanPromptBuilder;
    private final ObjectProvider<OpenAiClient> openAiClientProvider;
    private final ObjectProvider<TripStorageService> tripStorageServiceProvider;
    private final TripRepository tripRepository;

    // @Override
    // public void generateTripFromPreference(TripPreferenceRequestDTO dto) {
    //     Assert.notNull(dto, "Trip preference DTO must not be null");

    //     // 1. Convert DTO to entity
    //     TripPreference preference = modelMapper.map(dto, TripPreference.class);
    //     log.debug("Mapped trip preference request to entity: {}", preference);

    //     // 2. Check if weather needs fetching
    //     LocalDate startDate = preference.getStartDate();
    //     LocalDate weatherWindow = LocalDate.now().plusDays(5);
    //     boolean shouldFetchWeather = startDate != null && weatherWindow.isAfter(startDate);
    //     List<DailyWeatherDTO> weatherSummaries = List.of();

    //     if (shouldFetchWeather) {
    //         if (preference.getTripId() == null) {
    //             log.warn("Trip ID is null, skipping weather fetch for preference {}", preference);
    //         } else {
    //             log.debug("Fetching weather for trip {} starting {}", preference.getTripId(), startDate);
    //             weatherService.fetchAndStoreWeather(preference);
    //             List<TripWeather> storedWeather = tripWeatherRepository.findByTripId(preference.getTripId());
    //             weatherSummaries = storedWeather.stream()
    //                     .map(weather -> DailyWeatherDTO.builder()
    //                             .date(weather.getDate())
    //                             .minTemp(weather.getMinTemp())
    //                             .maxTemp(weather.getMaxTemp())
    //                             .weatherCondition(weather.getWeatherCondition())
    //                             .build())
    //                     .collect(Collectors.toList());
    //             log.debug("Retrieved {} weather summaries for trip {}", weatherSummaries.size(), preference.getTripId());
    //         }
    //     } else {
    //         log.debug("Skipping weather fetch. Trip start {}, threshold {}", startDate, weatherWindow);
    //     }

    //     // 3. Build the GPT prompt
    //     String prompt = promptBuilder.build(preference, weatherSummaries);
    //     log.debug("Constructed trip generation prompt");

    //     // 4. Call OpenAI to get trip plan JSON
    //     OpenAiClient openAiClient = Optional.ofNullable(openAiClientProvider.getIfAvailable())
    //             .orElseThrow(() -> new IllegalStateException("OpenAiClient bean is not configured"));
    //     String tripPlanJson = openAiClient.requestTripPlan(prompt);
    //     log.debug("Received trip plan JSON payload");

    //     // 5. Store the generated trip plan
    //     TripStorageService tripStorageService = Optional.ofNullable(tripStorageServiceProvider.getIfAvailable())
    //             .orElseThrow(() -> new IllegalStateException("TripStorageService bean is not configured"));
    //     tripStorageService.storeTripPlan(preference, tripPlanJson);

    //     log.info("Successfully generated and stored trip plan for user {}", preference.getUserId());
    // }

    @Override
    @Transactional
    public void generateTripAndReturnJson(TripPreferenceRequestDTO dto, String userId) {
        Assert.notNull(dto, "Trip preference DTO must not be null");

        // 1. Convert the request DTO into a TripPreference entity
        Trip preference = modelMapper.map(dto, Trip.class);
        preference.setUserId(Long.valueOf(userId));
        tripRepository.save(preference);
        log.debug("Mapped trip preference request to entity: {}", preference);

        // 2. Check if weather data is needed (only if trip starts within 5 days from today)
        LocalDate startDate = preference.getStartDate();
        LocalDate weatherWindow = LocalDate.now().plusDays(5);
        boolean shouldFetchWeather = startDate != null && weatherWindow.isAfter(startDate);
        List<DailyWeatherDTO> weatherSummaries = List.of();

        if (shouldFetchWeather) {
            if (preference.getId() == null) {
                log.warn("Trip ID is null, skipping weather fetch for preference {}", preference);
            } else {
                log.debug("Fetching weather for trip {} starting {}", preference.getId(), startDate);
                weatherService.fetchAndStoreWeather(preference);
                List<TripWeather> storedWeather = tripWeatherRepository.findByTripId(preference.getId());
                weatherSummaries = storedWeather.stream()
                        .map(weather -> DailyWeatherDTO.builder()
                                .date(weather.getDate())
                                .minTemp(weather.getMinTemp())
                                .maxTemp(weather.getMaxTemp())
                                .weatherCondition(weather.getWeatherCondition())
                                .build())
                        .collect(Collectors.toList());
                log.debug("Retrieved {} weather summaries for trip {}", weatherSummaries.size(), preference.getId());
            }
        } else {
            log.debug("Skipping weather fetch. Trip start {}, threshold {}", startDate, weatherWindow);
        }

        // 3. Build the prompt for OpenAI based on trip preference and weather
        String prompt = tripPlanPromptBuilder.build(preference, weatherSummaries);
        log.debug("Constructed trip generation prompt");

        // 4. Get OpenAiClient and call GPT API using the built prompt
        OpenAiClient openAiClient = Optional.ofNullable(openAiClientProvider.getIfAvailable())
                .orElseThrow(() -> new IllegalStateException("OpenAiClient bean is not configured"));
        String tripPlanJson = openAiClient.requestTripPlan(prompt);
        log.debug("Received trip plan JSON payload：{}", tripPlanJson);

        ItineraryDTO itineraryDTO = openAiClient.parseContent(tripPlanJson, ItineraryDTO.class);
        if (itineraryDTO == null) {
            throw new IllegalStateException("parse failed");
        }
        String innerJson = null;
        try {
            innerJson = objectMapper.writeValueAsString(itineraryDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 5. Store the generated trip plan using TripStorageService
        TripStorageService tripStorageService = Optional.ofNullable(tripStorageServiceProvider.getIfAvailable())
                .orElseThrow(() -> new IllegalStateException("TripStorageService bean is not configured"));
        tripStorageService.storeTripPlan(preference, innerJson);

        log.info("Generated Trip JSON:\n{}", innerJson);
        log.info("Successfully generated and stored trip plan for user {}", preference.getUserId());
    }

}