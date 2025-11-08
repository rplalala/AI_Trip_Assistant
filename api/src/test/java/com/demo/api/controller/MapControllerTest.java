package com.demo.api.controller;

import com.demo.api.dto.MapRouteRequest;
import com.demo.api.dto.MapRouteResponse;
import com.demo.api.enums.MapProvider;
import com.demo.api.service.MapService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MapControllerTest {

    @Mock
    private MapService mapService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MapController controller = new MapController(mapService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @DisplayName("POST /api/map/route delegates to MapService and returns response payload")
    @Test
    void generateRoute_returnsServiceResult() throws Exception {
        MapRouteRequest request = new MapRouteRequest("Sydney", "Canberra", "driving", "google");
        MapRouteResponse response = MapRouteResponse.builder()
                .provider(MapProvider.GOOGLE)
                .travelMode("driving")
                .routeSummary("Sydney -> Canberra")
                .distanceText("123 km")
                .distanceMeters(123456)
                .durationText("1 hour 40 mins")
                .durationSeconds(6000)
                .overviewPolyline("abcd")
                .embedUrl("https://maps/embed")
                .shareUrl("https://maps/share")
                .warnings(List.of("Avoid tolls"))
                .build();

        when(mapService.mapProviderSelect(eq(request.getProvider()))).thenReturn(MapProvider.GOOGLE);
        when(mapService.generateRoute(any(MapRouteRequest.class), eq(MapProvider.GOOGLE))).thenReturn(response);

        mockMvc.perform(post("/api/map/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.routeSummary").value("Sydney -> Canberra"))
                .andExpect(jsonPath("$.data.distanceText").value("123 km"));

        ArgumentCaptor<MapRouteRequest> captor = ArgumentCaptor.forClass(MapRouteRequest.class);
        verify(mapService).mapProviderSelect(eq(request.getProvider()));
        verify(mapService).generateRoute(captor.capture(), eq(MapProvider.GOOGLE));
        assertThat(captor.getValue().getOrigin()).isEqualTo("Sydney");
        assertThat(captor.getValue().getDestination()).isEqualTo("Canberra");
        assertThat(captor.getValue().getTravelMode()).isEqualTo("driving");
    }
}
