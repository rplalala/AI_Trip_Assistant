package com.demo.api.service.impl;

import com.demo.api.dto.MapRouteRequest;
import com.demo.api.dto.MapRouteResponse;
import com.demo.api.enums.MapProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        mapService = new MapServiceImpl(restTemplate, new ObjectMapper());
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

        MapRouteRequest request = new MapRouteRequest("Sydney", "Parramatta", "driving", "google");
        MapRouteResponse response = mapService.generateRoute(request, MapProvider.GOOGLE);

        server.verify();
        assertThat(response.getRouteSummary()).isEqualTo("Harbour Bridge");
        assertThat(response.getDistanceText()).isEqualTo("12 km");
        assertThat(response.getWarnings()).contains("Congestion ahead", "Tolls apply");
        assertThat(response.getProvider()).isEqualTo(MapProvider.GOOGLE);
        assertThat(response.getEmbedUrl()).contains("key=test-key");
    }

    @Test
    void generateRoute_whenGoogleReturnsError_buildsFallbackResponse() {
        String responseJson = """
                {"status":"ZERO_RESULTS","error_message":"No routes found"}
                """;
        server.expect(ExpectedCount.once(), requestTo(Matchers.containsString("maps.googleapis.com")))
                .andRespond(withSuccess(responseJson, APPLICATION_JSON));

        MapRouteRequest request = new MapRouteRequest("Sydney", "Melbourne", "driving", "google");
        MapRouteResponse response = mapService.generateRoute(request, MapProvider.GOOGLE);

        server.verify();
        assertThat(response.getEmbedUrl()).isNull();
        assertThat(response.getShareUrl()).contains("travelmode=driving");
        assertThat(response.getProvider()).isEqualTo(MapProvider.GOOGLE);
        assertThat(response.getWarnings()).contains("No routes found");
    }

    @Test
    void generateRoute_whenModeUnsupported_throwsIllegalArgument() {
        MapRouteRequest request = new MapRouteRequest("A", "B", "flying", "google");
        assertThatThrownBy(() -> mapService.generateRoute(request, MapProvider.GOOGLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported travel mode");
    }

    @Test
    void generateRoute_whenGoogleApiKeyMissing_throwsIllegalState() {
        ReflectionTestUtils.setField(mapService, "mapsApiKey", "");
        MapRouteRequest request = new MapRouteRequest("A", "B", "driving", "google");

        assertThatThrownBy(() -> mapService.generateRoute(request, MapProvider.GOOGLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google Maps API key is not configured");
    }

    @Test
    void generateRoute_withAmapProvider_returnsRouteData() {
        ReflectionTestUtils.setField(mapService, "amapServiceApiKey", "amap-key");
        // Google Geocoding stubs (WGS84)
        String googleGeoOrigin = """
                {"status":"OK","results":[{"formatted_address":"Origin CN","geometry":{"location":{"lat":39.989643,"lng":116.481028}}}]} 
                """;
        String googleGeoDest = """
                {"status":"OK","results":[{"formatted_address":"Destination CN","geometry":{"location":{"lat":40.004717,"lng":116.465302}}}]} 
                """;
        server.expect(ExpectedCount.once(), requestTo(Matchers.containsString("maps.googleapis.com/maps/api/geocode/json")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(googleGeoOrigin, APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(Matchers.containsString("maps.googleapis.com/maps/api/geocode/json")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(googleGeoDest, APPLICATION_JSON));

        // AMap walking directions stub
        String directions = """
                {
                  "status":"1",
                  "info":"OK",
                  "route":{"paths":[{"distance":"1500","duration":"600"}]}
                }
                """;
        server.expect(ExpectedCount.once(), requestTo(Matchers.containsString("restapi.amap.com/v3/direction/walking")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(directions, APPLICATION_JSON));

        MapRouteRequest request = new MapRouteRequest("三里屯", "首都机场", "walking", "amap");
        MapRouteResponse response = mapService.generateRoute(request, MapProvider.AMAP);

        server.verify();
        assertThat(response.getProvider()).isEqualTo(MapProvider.AMAP);
        assertThat(response.getDistanceText()).isEqualTo("1.5 km");
        assertThat(response.getDurationText()).isEqualTo("10 min");
        assertThat(response.getShareUrl()).contains("uri.amap.com");
        assertThat(response.getEmbedUrl()).contains("uri.amap.com");
    }

    @Test
    void generateRoute_whenAmapApiKeyMissing_throwsIllegalState() {
        MapRouteRequest request = new MapRouteRequest("A", "B", "walking", "amap");
        assertThatThrownBy(() -> mapService.generateRoute(request, MapProvider.AMAP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AMap API key is not configured");
    }
}
