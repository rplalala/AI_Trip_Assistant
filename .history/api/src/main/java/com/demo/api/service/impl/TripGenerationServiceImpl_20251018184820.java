package com.demo.api.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.demo.api.client.OpenAiClient;
import com.demo.api.dto.DailyWeatherDTO;
import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.model.TripPreference;
import com.demo.api.model.TripWeather;
import com.demo.api.repository.TripWeatherRepository;
import com.demo.api.service.TripGenerationService;
import com.demo.api.service.TripStorageService;
import com.demo.api.service.WeatherService;
import com.demo.api.service.prompt.PromptBuilder;

import lombok.RequiredArgsConstructor;

/**
 * Coordinates trip generation by chaining weather lookup, prompt creation, and GPT invocation.
 */
@Service
@RequiredArgsConstructor
public class TripGenerationServiceImpl implements TripGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TripGenerationServiceImpl.class);

    private final ModelMapper modelMapper;
    private final WeatherService weatherService;
    private final TripWeatherRepository tripWeatherRepository;
    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;
    private final TripStorageService tripStorageService;

    @Override
    public void generateTripFromPreference(TripPreferenceRequestDTO dto) {
        Assert.notNull(dto, "Trip preference DTO must not be null");

        // 1. Convert DTO to entity
        TripPreference preference = modelMapper.map(dto, TripPreference.class);
        log.debug("Mapped trip preference request to entity: {}", preference);

        // 2. Decide whether weather is required (within 5-day window)
        LocalDate startDate = preference.getStartDate();
        LocalDate weatherWindow = LocalDate.now().plusDays(5);
        boolean shouldFetchWeather = startDate != null && weatherWindow.isAfter(startDate);
        List<DailyWeatherDTO> weatherSummaries = List.of();

        if (shouldFetchWeather) {
            if (preference.getTripId() == null) {
                log.warn("Trip ID is null, skipping weather fetch for preference {}", preference);
            } else {
                log.debug("Fetching weather for trip {} starting {}", preference.getTripId(), startDate);
                weatherService.fetchAndStoreWeather(preference);
                List<TripWeather> storedWeather = tripWeatherRepository.findByTripId(preference.getTripId());
                weatherSummaries = storedWeather.stream()
                        .map(weather -> DailyWeatherDTO.builder()
                                .date(weather.getDate())
                                .minTemp(weather.getMinTemp())
                                .maxTemp(weather.getMaxTemp())
                                .weatherCondition(weather.getWeatherCondition())
                                .build())
                        .collect(Collectors.toList());
                log.debug("Retrieved {} weather summaries for trip {}", weatherSummaries.size(), preference.getTripId());
            }
        } else {
            log.debug("Skipping weather fetch. Trip start {}, threshold {}", startDate, weatherWindow);
        }

        // 4. Build GPT prompt
        String prompt = promptBuilder.build(preference, weatherSummaries);
        log.debug("Constructed trip generation prompt");

        // 5. Request trip plan from OpenAI
        String tripPlanJson = openAiClient.requestTripPlan(prompt);
        log.debug("Received trip plan JSON payload");

        // 6. Persist generated plan
        tripStorageService.storeTripPlan(preference, tripPlanJson);
        log.info("Successfully generated and stored trip plan for user {}", preference.getUserId());
    }
}

