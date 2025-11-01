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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TripInsightServiceImplTest {

    @Mock private TripInsightRepository tripInsightRepository;
    @Mock private TripRepository tripRepository;
    @Mock private TripAttractionRepository tripAttractionRepository;
    @Mock private ObjectProvider<OpenAiClient> openAiClientProvider;
    @Mock private OpenAiClient openAiClient;

    private TripInsightServiceImpl tripInsightService;

    @BeforeEach
    void setUp() {
        tripInsightService = new TripInsightServiceImpl(
                new TripInsightMapper(),
                tripInsightRepository,
                tripRepository,
                tripAttractionRepository,
                openAiClientProvider
        );
    }

    @Test
    void generateAndStoreInsights_whenItineraryAvailable_persistsInsights() {
        Trip trip = Trip.builder()
                .id(12L)
                .toCity("Kyoto")
                .toCountry("Japan")
                .startDate(LocalDate.of(2025, 5, 1))
                .endDate(LocalDate.of(2025, 5, 5))
                .build();
        when(tripRepository.findById(12L)).thenReturn(Optional.of(trip));
        when(tripAttractionRepository.findByTripId(12L)).thenReturn(List.of(
                TripAttraction.builder().location("Fushimi Inari").build()
        ));
        when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
        when(openAiClient.requestTripPlan(anyString())).thenReturn("{\"insights\":[]}");
        InsightResponseDTO.InsightItem item = new InsightResponseDTO.InsightItem();
        item.setId("1");
        item.setTitle("Shrine Etiquette");
        item.setContent("Be respectful at shrines.");
        item.setTheme("culture");
        item.setIcon("⛩️");
        InsightResponseDTO response = new InsightResponseDTO();
        response.setInsights(List.of(item));
        when(openAiClient.parseContent(anyString(), eq(InsightResponseDTO.class))).thenReturn(response);
        when(tripInsightRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<TripInsight> saved = invocation.getArgument(0);
            long counter = 1L;
            for (TripInsight insight : saved) {
                if (insight.getId() == null) {
                    insight.setId(counter++);
                }
            }
            return saved;
        });

        List<TripInsight> results = tripInsightService.generateAndStoreInsights(12L);

        assertThat(results).hasSize(1);
        TripInsight insight = results.getFirst();
        assertThat(insight.getTripId()).isEqualTo(12L);
        assertThat(insight.getTitle()).isEqualTo("Shrine Etiquette");

        ArgumentCaptor<List<TripInsight>> captor = ArgumentCaptor.forClass(List.class);
        verify(tripInsightRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        verify(openAiClient).requestTripPlan(contains("Kyoto"));
    }

    @Test
    void generateAndStoreInsights_whenNoAttractions_returnsEmpty() {
        Trip trip = Trip.builder().id(44L).build();
        when(tripRepository.findById(44L)).thenReturn(Optional.of(trip));
        when(tripAttractionRepository.findByTripId(44L)).thenReturn(List.of());

        List<TripInsight> results = tripInsightService.generateAndStoreInsights(44L);

        assertThat(results).isEmpty();
        verifyNoInteractions(openAiClientProvider);
    }

    @Test
    void getOrGenerateInsights_whenCached_returnsDtoList() {
        TripInsight stored = new TripInsight();
        stored.setId(5L);
        stored.setTripId(22L);
        stored.setTitle("History");
        stored.setContent("Rich history");
        stored.setTheme("culture");
        stored.setIcon("🏯");

        when(tripInsightRepository.existsByTripId(22L)).thenReturn(true);
        when(tripInsightRepository.findByTripIdOrderById(22L)).thenReturn(List.of(stored));

        List<TripInsightDTO> dtos = tripInsightService.getOrGenerateInsights(22L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.getFirst().getTitle()).isEqualTo("History");
        verify(tripInsightRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrGenerateInsights_whenNotCached_triggersGeneration() {
        Trip trip = Trip.builder().id(30L).build();
        TripAttraction attraction = TripAttraction.builder().location("Sydney Harbour").build();
        when(tripInsightRepository.existsByTripId(30L)).thenReturn(false);
        when(tripRepository.findById(30L)).thenReturn(Optional.of(trip));
        when(tripAttractionRepository.findByTripId(30L)).thenReturn(List.of(attraction));
        when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
        when(openAiClient.requestTripPlan(anyString())).thenReturn("{\"insights\":[]}");
        InsightResponseDTO response = new InsightResponseDTO();
        InsightResponseDTO.InsightItem item = new InsightResponseDTO.InsightItem();
        item.setId("1");
        item.setTitle("Harbour Cruise");
        item.setContent("Enjoy the harbour.");
        item.setIcon("⛴️");
        item.setTheme("scenery");
        response.setInsights(List.of(item));
        when(openAiClient.parseContent(anyString(), eq(InsightResponseDTO.class))).thenReturn(response);
        when(tripInsightRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<TripInsight> saved = invocation.getArgument(0);
            long counter = 1L;
            for (TripInsight insight : saved) {
                if (insight.getId() == null) {
                    insight.setId(counter++);
                }
            }
            return saved;
        });

        List<TripInsightDTO> results = tripInsightService.getOrGenerateInsights(30L);

        assertThat(results).hasSize(1);
        verify(tripInsightRepository).saveAll(anyList());
    }
}
