package com.demo.api.service.impl;

import com.demo.api.client.OpenAiClient;
import com.demo.api.dto.InsightResponseDTO;
import com.demo.api.dto.TripInsightDTO;
import com.demo.api.mapper.TripInsightMapper;
import com.demo.api.model.Trip;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripInsight;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripInsightRepository;
import com.demo.api.repository.TripRepository;
import com.demo.api.service.TripInsightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TripInsightServiceImpl implements TripInsightService {

    private static final Logger log = LoggerFactory.getLogger(TripInsightServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TripInsightMapper tripInsightMapper;

    private final TripInsightRepository insightRepository;

    private final TripRepository tripRepository;

    private final TripAttractionRepository tripAttractionRepository;

    private final ObjectProvider<OpenAiClient> openAiClientProvider;

    public TripInsightServiceImpl(TripInsightMapper tripInsightMapper, TripInsightRepository insightRepository, TripRepository tripRepository, TripAttractionRepository tripAttractionRepository, ObjectProvider<OpenAiClient> openAiClientProvider) {
        this.tripInsightMapper = tripInsightMapper;
        this.insightRepository = insightRepository;
        this.tripRepository = tripRepository;
        this.tripAttractionRepository = tripAttractionRepository;
        this.openAiClientProvider = openAiClientProvider;
    }


    @Override
    @Transactional
    public List<TripInsight> generateAndStoreInsights(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        List<TripAttraction> tripAttractions = tripAttractionRepository.findByTripId(tripId);
        if (tripAttractions.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> locations = tripAttractions.stream().map(TripAttraction::getLocation).toList();
        String userPrompt = buildUserPrompt(trip, locations);

        OpenAiClient openAiClient = Optional.ofNullable(openAiClientProvider.getIfAvailable())
                .orElseThrow(() -> new IllegalStateException("OpenAiClient bean is not configured"));
        String InsightsJson = openAiClient.requestTripPlan(userPrompt);
        log.info("Received Insights Json: {}", InsightsJson);

        InsightResponseDTO insightResponse = openAiClient.parseContent(InsightsJson, InsightResponseDTO.class);

        List<TripInsight> results = new ArrayList<>();
        if (insightResponse != null) {
            insightResponse.getInsights().forEach(insight -> {
                TripInsight i = new TripInsight();
                i.setTripId(trip.getId());
                i.setTitle(insight.getTitle());
                i.setContent(insight.getContent());
                i.setTheme(insight.getTheme());
                i.setIcon(insight.getIcon());
                results.add(i);
            });
        }
        return insightRepository.saveAll(results);
    }

    @Override
    @Transactional
    public List<TripInsightDTO> getOrGenerateInsights(Long tripId) {
        List<TripInsight> insights = Collections.emptyList();

        if (insightRepository.existsByTripId(tripId)) {
            insights =  insightRepository.findByTripIdOrderById(tripId);
        } else {
            insights = generateAndStoreInsights(tripId);
        }

        return tripInsightMapper.toDtoList(insights);
    }

    private String buildUserPrompt(Trip trip, List<String> pois) {
        return """
        Produce short, concrete destination insights tied to the user's itinerary.
        Each item: title, content (2–3 sentences), theme, and an emoji icon representing the theme.
        Avoid prices or live data.
        Output strictly valid JSON per schema. No extra text.
        Do NOT include any markdown formatting, code fences, or language hints.
        Return only raw JSON text starting with '{' and ending with '}'.

        Destination: %s, %s
        Dates: %s → %s
        Tone: magazine
        Max items: 6
        Preferred themes: ["history","culture","food","etiquette","nature","festival"]
        Top POIs in plan: %s

        Task:
        1) Create up to 6 insights aligned with the POIs/season.
        2) 60–70%% evergreen facts, 30–40%% contextual tips.
        3) Return JSON: {"insights":[{id,title,content,theme,icon}], "meta":{destination,dates,generated_at}}
        Return JSON only.
        """.formatted(
                trip.getToCity(), trip.getToCountry(),
                trip.getStartDate(), trip.getEndDate(),
                pois.toString()
        );
    }
}
