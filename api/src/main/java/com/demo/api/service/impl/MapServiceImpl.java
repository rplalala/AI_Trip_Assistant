package com.demo.api.service.impl;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.demo.api.dto.map.MapRouteRequest;
import com.demo.api.dto.map.MapRouteResponse;
import com.demo.api.service.MapService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Service that calls Google Directions API and assembles route metadata for the frontend.
 */
@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

    private static final String DEFAULT_MODE = "driving";
    private static final Set<String> SUPPORTED_MODES = Set.of("driving", "walking", "bicycling", "transit");
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String EMBED_BASE_URL = "https://www.google.com/maps/embed/v1/directions";
    private static final String SHARE_BASE_URL = "https://www.google.com/maps/dir/";

    private final RestTemplate restTemplate;

    @Value("${google.maps.api-key:}")
    private String mapsApiKey;

    @Override
    public MapRouteResponse generateRoute(MapRouteRequest request) {
        validateRequest(request);

        String normalizedMode = normalizeMode(request.getTravelMode());
        String origin = request.getOrigin().trim();
        String destination = request.getDestination().trim();

        try {
            GoogleDirectionsResponse directions = callDirectionsApi(origin, destination, normalizedMode);
            Route route = directions.firstRoute()
                    .orElseThrow(() -> new IllegalStateException("No routes returned by Google Maps"));

            Leg firstLeg = route.firstLeg()
                    .orElseThrow(() -> new IllegalStateException("Route did not contain any legs"));

            MapRouteResponse response = MapRouteResponse.builder()
                    .travelMode(normalizedMode)
                    .routeSummary(route.getSummary())
                    .distanceText(Optional.ofNullable(firstLeg.getDistance()).map(TextValue::getText).orElse(null))
                    .distanceMeters(Optional.ofNullable(firstLeg.getDistance()).map(TextValue::getValue).orElse(null))
                    .durationText(Optional.ofNullable(firstLeg.getDuration()).map(TextValue::getText).orElse(null))
                    .durationSeconds(Optional.ofNullable(firstLeg.getDuration()).map(TextValue::getValue).orElse(null))
                    .overviewPolyline(Optional.ofNullable(route.getOverviewPolyline()).map(Polyline::getPoints).orElse(null))
                    .embedUrl(buildEmbedUrl(origin, destination, normalizedMode))
                    .shareUrl(buildShareUrl(origin, destination, normalizedMode))
                    .warnings(collectWarnings(directions, route))
                    .build();

            log.debug("Generated Google Maps route (mode={}, distance={}, duration={})",
                    normalizedMode, response.getDistanceText(), response.getDurationText());
            return response;
        } catch (IllegalStateException ex) {
            log.warn("Falling back to share-only route for origin='{}', destination='{}': {}", origin, destination, ex.getMessage());
            return buildFallbackResponse(origin, destination, normalizedMode, ex.getMessage());
        }
    }

    private void validateRequest(MapRouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!StringUtils.hasText(request.getOrigin())) {
            throw new IllegalArgumentException("origin cannot be blank");
        }
        if (!StringUtils.hasText(request.getDestination())) {
            throw new IllegalArgumentException("destination cannot be blank");
        }
        if (!StringUtils.hasText(mapsApiKey)) {
            throw new IllegalStateException("Google Maps API key is not configured");
        }
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return DEFAULT_MODE;
        }
        String normalized = mode.trim().toLowerCase(Locale.ENGLISH);
        if (!SUPPORTED_MODES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported travel mode: " + mode);
        }
        return normalized;
    }

    private GoogleDirectionsResponse callDirectionsApi(String origin, String destination, String mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(DIRECTIONS_URL)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("mode", mode)
                .queryParam("key", mapsApiKey)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        try {
            GoogleDirectionsResponse response = restTemplate.getForObject(uri, GoogleDirectionsResponse.class);
            if (response == null) {
                throw new IllegalStateException("Empty response from Google Maps");
            }
            if (!"OK".equalsIgnoreCase(response.getStatus())) {
                String errorMessage = Optional.ofNullable(response.getErrorMessage())
                        .orElse("Google Maps returned status: " + response.getStatus());
                throw new IllegalStateException(errorMessage);
            }
            return response;
        } catch (RestClientResponseException ex) {
            String message = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("Google Maps API error: status={}, body={}", ex.getRawStatusCode(), message);
            throw new IllegalStateException("Failed to reach Google Maps Directions API: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("Google Maps API invocation failed", ex);
            throw new IllegalStateException("Unable to call Google Maps Directions API", ex);
        }
    }

    private String buildEmbedUrl(String origin, String destination, String mode) {
        return UriComponentsBuilder.fromHttpUrl(EMBED_BASE_URL)
                .queryParam("key", mapsApiKey)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("mode", mode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private String buildShareUrl(String origin, String destination, String mode) {
        return UriComponentsBuilder.fromHttpUrl(SHARE_BASE_URL)
                .queryParam("api", 1)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("travelmode", mode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private List<String> collectWarnings(GoogleDirectionsResponse directions,
                                         Route route) {
        Stream<String> topLevel = Optional.ofNullable(directions.getWarnings())
                .stream()
                .flatMap(List::stream)
                .filter(StringUtils::hasText);
        Stream<String> routeWarnings = Optional.ofNullable(route.getWarnings())
                .stream()
                .flatMap(List::stream)
                .filter(StringUtils::hasText);
        return Stream.concat(topLevel, routeWarnings)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private MapRouteResponse buildFallbackResponse(String origin,
                                                   String destination,
                                                   String mode,
                                                   String reason) {
        List<String> warnings = new ArrayList<>();
        warnings.add("Google Maps could not provide a detailed route for this request. Showing only the share link.");
        if (StringUtils.hasText(reason)) {
            warnings.add(reason);
        }
        return MapRouteResponse.builder()
                .travelMode(mode)
                .embedUrl(null)
                .shareUrl(buildShareUrl(origin, destination, mode))
                .warnings(warnings)
                .build();
    }

    // ----------- Internal DTOs for Google Directions response -----------

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoogleDirectionsResponse {
        private String status;
        @JsonProperty("error_message")
        private String errorMessage;
        private List<Route> routes;
        private List<String> warnings;

        Optional<Route> firstRoute() {
            return Optional.ofNullable(routes)
                    .stream()
                    .flatMap(List::stream)
                    .findFirst();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Route {
        private String summary;
        private List<Leg> legs;
        @JsonProperty("overview_polyline")
        private Polyline overviewPolyline;
        private List<String> warnings;

        Optional<Leg> firstLeg() {
            return Optional.ofNullable(legs)
                    .stream()
                    .flatMap(List::stream)
                    .findFirst();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Leg {
        private TextValue distance;
        private TextValue duration;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TextValue {
        private String text;
        private Integer value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Polyline {
        private String points;
    }
}
