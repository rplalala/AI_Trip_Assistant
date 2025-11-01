package com.demo.api.controller;

import com.demo.api.dto.ModifyPlanDTO;
import com.demo.api.dto.TimeLineDTO;
import com.demo.api.dto.TripDetailDTO;
import com.demo.api.dto.TripInsightDTO;
import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.service.TripGenerationService;
import com.demo.api.service.TripInsightService;
import com.demo.api.service.TripService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TripControllerTest {

    @Mock
    private TripGenerationService tripGenerationService;
    @Mock
    private TripInsightService tripInsightService;
    @Mock
    private TripService tripService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TripController controller = new TripController(tripGenerationService, tripInsightService, tripService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor withUser(String userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(userId, null));
            request.setUserPrincipal(() -> userId);
            return request;
        };
    }

    @Test
    void generate_plan_delegates_to_service() throws Exception {
        TripPreferenceRequestDTO dto = new TripPreferenceRequestDTO();
        dto.setFromCity("Sydney");
        dto.setToCity("Tokyo");

        mockMvc.perform(post("/api/trip/generate-plan")
                        .with(withUser("42"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(tripGenerationService).generateTripAndReturnJson(dto, "42");
    }

    @Test
    void regenerate_plan_delegates_to_service() throws Exception {
        ModifyPlanDTO modifyPlanDTO = new ModifyPlanDTO();
        modifyPlanDTO.setSecondPreference("more food");

        mockMvc.perform(post("/api/trip/regenerate-plan")
                        .with(withUser("77"))
                        .param("tripId", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(modifyPlanDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(tripGenerationService).regenerateTrip(12L, modifyPlanDTO, "77");
    }

    @Test
    void get_trip_details_returns_service_result() throws Exception {
        TripDetailDTO detail = new TripDetailDTO();
        detail.setTripId(15L);
        when(tripService.getTripDetails(55L)).thenReturn(List.of(detail));

        mockMvc.perform(get("/api/trip/details")
                        .with(withUser("55")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].tripId").value(15L));

        verify(tripService).getTripDetails(55L);
    }

    @Test
    void get_insights_returns_dtos() throws Exception {
        when(tripInsightService.getOrGenerateInsights(9L))
                .thenReturn(List.of(new TripInsightDTO("1", "Title", "Content", "theme", "icon")));

        mockMvc.perform(get("/api/trip/insights").param("tripId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Title"));

        verify(tripInsightService).getOrGenerateInsights(9L);
    }

    @Test
    void get_timeline_returns_timeline() throws Exception {
        TimeLineDTO timeline = new TimeLineDTO();
        when(tripService.getTimeLine(25L)).thenReturn(List.of(timeline));

        mockMvc.perform(get("/api/trip/timeline").param("tripId", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(tripService).getTimeLine(25L);
    }

    @Test
    void delete_trips_delegates_to_service() throws Exception {
        mockMvc.perform(delete("/api/trip").param("tripIds", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(tripService).deleteTripByIds(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue()).containsExactly(1L, 2L);
    }
}
