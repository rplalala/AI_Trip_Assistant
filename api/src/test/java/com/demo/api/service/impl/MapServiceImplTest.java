package com.demo.api.service.impl;

import com.demo.api.dto.MapRouteRequest;
import com.demo.api.dto.MapRouteResponse;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.hamcrest.Matchers;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MapServiceImplTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private MapServiceImpl mapService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        mapService = new MapServiceImpl(restTemplate);
        ReflectionTestUtils.setField(mapService, "mapsApiKey", "test-key");
    }

    @Test
    void generateRoute_withOkResponse_returnsDetailedRoute() {
        String responseJson = """
                {
                  "status":"OK",
                  "warnings":["Congestion ahead"],
                  "routes":[
                    {
                      "summary":"Harbour Bridge",
                      "legs":[
                        {"distance":{"text":"12 km","value":12000},"duration":{"text":"20 mins","value":1200}}
                      ],
                      "overview_polyline":{"points":"abcd"},
                      "warnings":["Tolls apply"]
                    }
                  ]
                }
                """;
        server.expect(ExpectedCount.once(),
                        requestTo(Matchers.containsString("maps.googleapis.com")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(responseJson, APPLICATION_JSON));

        MapRouteRequest request = new MapRouteRequest("Sydney", "Parramatta", "driving");
        MapRouteResponse response = mapService.generateRoute(request);

        server.verify();
        assertThat(response.getRouteSummary()).isEqualTo("Harbour Bridge");
        assertThat(response.getDistanceText()).isEqualTo("12 km");
        assertThat(response.getWarnings()).contains("Congestion ahead", "Tolls apply");
        assertThat(response.getEmbedUrl()).contains("key=test-key");
    }

    @Test
    void generateRoute_whenGoogleReturnsError_buildsFallbackResponse() {
        String responseJson = """
                {"status":"ZERO_RESULTS","error_message":"No routes found"}
                """;
        server.expect(ExpectedCount.once(), requestTo(Matchers.containsString("maps.googleapis.com")))
                .andRespond(withSuccess(responseJson, APPLICATION_JSON));

        MapRouteRequest request = new MapRouteRequest("Sydney", "Melbourne", "driving");
        MapRouteResponse response = mapService.generateRoute(request);

        server.verify();
        assertThat(response.getEmbedUrl()).isNull();
        assertThat(response.getShareUrl()).contains("travelmode=driving");
        assertThat(response.getWarnings()).contains("No routes found");
    }

    @Test
    void generateRoute_whenModeUnsupported_throwsIllegalArgument() {
        MapRouteRequest request = new MapRouteRequest("A", "B", "flying");
        assertThatThrownBy(() -> mapService.generateRoute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported travel mode");
    }

    @Test
    void generateRoute_whenApiKeyMissing_throwsIllegalState() {
        ReflectionTestUtils.setField(mapService, "mapsApiKey", "");
        MapRouteRequest request = new MapRouteRequest("A", "B", "driving");

        assertThatThrownBy(() -> mapService.generateRoute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google Maps API key is not configured");
    }
}
