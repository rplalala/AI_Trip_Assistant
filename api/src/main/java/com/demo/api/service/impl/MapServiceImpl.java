package com.demo.api.service.impl;

import com.demo.api.dto.MapRouteRequest;
import com.demo.api.dto.MapRouteResponse;
import com.demo.api.enums.MapProvider;
import com.demo.api.service.MapService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service that calls Google Directions API and AMap Web APIs and assembles route metadata.
 */
@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

    private static final String DEFAULT_MODE = "driving";
    private static final Set<String> SUPPORTED_MODES = Set.of("driving", "walking", "bicycling", "transit");

    // 实际上是从前端发起请求到外部服务，后端只是在链接后面拼接了参数，形成完整链接并返回给前端。所以 响应速度/是否能够响应 取决于用户的网络环境。
    // 注意：Google Embed 会拼接上 Google Map 的 Api key，并且这是无法避免的，只能设置权限。https://github.com/shukerullah/react-geocode/issues/49
    private static final String GOOGLE_DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String GOOGLE_EMBED_BASE_URL = "https://www.google.com/maps/embed/v1/directions";
    private static final String GOOGLE_SHARE_BASE_URL = "https://www.google.com/maps/dir/";
    //===================================
    // 下面这些链接都是从后端发起请求到外部服务商的，所以 响应速度/是否能够响应 取决于后端服务器的位置。
    // 有用到 RestTemplate（Spring 提供的一个 HTTP 客户端，用很少的代码发起 GET/POST/PUT/DELETE 等请求） 就代表是从后端发起请求的
    private static final String GOOGLE_GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String AMAP_GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final String AMAP_DRIVING_URL = "https://restapi.amap.com/v3/direction/driving";
    private static final String AMAP_WALKING_URL = "https://restapi.amap.com/v3/direction/walking";
    private static final String AMAP_TRANSIT_URL = "https://restapi.amap.com/v3/direction/transit/integrated";
    private static final String AMAP_BICYCLING_URL = "https://restapi.amap.com/v4/direction/bicycling";
    private static final String AMAP_SHARE_BASE_URL = "https://uri.amap.com/navigation";

    private static final Pattern COORDINATE_PATTERN =
            Pattern.compile("^\\s*-?\\d+(?:\\.\\d+)?\\s*,\\s*-?\\d+(?:\\.\\d+)?\\s*$");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.maps.api-key:}")
    private String mapsApiKey;

    @Value("${amap.web-service.api-key:}")
    private String amapServiceApiKey;

    /**
     * Entry point: validates request, normalizes travel mode, then dispatches to provider-specific generator.
     * @param request route request containing origin, destination, travelMode
     * @param provider selected map provider (GOOGLE / AMAP)
     * @return assembled MapRouteResponse (may be fallback if provider fails)
     */
    @Override
    public MapRouteResponse generateRoute(MapRouteRequest request, MapProvider provider) {
        validateCommonRequest(request);

        String normalizedMode = normalizeMode(request.getTravelMode());
        String origin = request.getOrigin().trim();
        String destination = request.getDestination().trim();

        return switch (provider) {
            case AMAP -> generateAmapRoute(origin, destination, normalizedMode);
            case GOOGLE -> generateGoogleRoute(origin, destination, normalizedMode);
        };
    }

    /**
     * Parses requested provider id into enum with graceful fallback to GOOGLE for unknown / blank values.
     * @param requestedProvider raw provider id from client
     * @return resolved MapProvider enum (defaults to GOOGLE)
     */
    @Override
    public MapProvider mapProviderSelect(String requestedProvider) {
        if (StringUtils.hasText(requestedProvider)) {
            try {
                return MapProvider.fromId(requestedProvider);
            } catch (IllegalArgumentException ex) {
                // Fallback to GOOGLE if an unknown id is supplied
                return MapProvider.GOOGLE;
            }
        }
        return MapProvider.GOOGLE;
    }

    /**
     * Shared validation: ensures request object and origin/destination strings are non blank.
     * @param request incoming route request
     * @throws IllegalArgumentException if required data missing
     */
    private void validateCommonRequest(MapRouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!StringUtils.hasText(request.getOrigin())) {
            throw new IllegalArgumentException("origin cannot be blank");
        }
        if (!StringUtils.hasText(request.getDestination())) {
            throw new IllegalArgumentException("destination cannot be blank");
        }
    }

    /**
     * Invokes Google Directions API and builds rich route response; falls back to share-only if no legs.
     * @param origin origin place (string or coordinate)
     * @param destination destination place
     * @param normalizedMode validated travel mode
     * @return detailed MapRouteResponse for Google or fallback
     */
    private MapRouteResponse generateGoogleRoute(String origin, String destination, String normalizedMode) {
        ensureGoogleConfigured();
        try {
            GoogleDirectionsResponse directions = callGoogleDirections(origin, destination, normalizedMode);
            Route route = directions.firstRoute()
                    .orElseThrow(() -> new IllegalStateException("No routes returned by Google Maps"));

            Leg firstLeg = route.firstLeg()
                    .orElseThrow(() -> new IllegalStateException("Route did not contain any legs"));

            MapRouteResponse response = MapRouteResponse.builder()
                    .provider(MapProvider.GOOGLE)
                    .travelMode(normalizedMode)
                    .routeSummary(route.getSummary())
                    .distanceText(Optional.ofNullable(firstLeg.getDistance()).map(TextValue::getText).orElse(null))
                    .distanceMeters(Optional.ofNullable(firstLeg.getDistance()).map(TextValue::getValue).orElse(null))
                    .durationText(Optional.ofNullable(firstLeg.getDuration()).map(TextValue::getText).orElse(null))
                    .durationSeconds(Optional.ofNullable(firstLeg.getDuration()).map(TextValue::getValue).orElse(null))
                    .overviewPolyline(Optional.ofNullable(route.getOverviewPolyline()).map(Polyline::getPoints).orElse(null))
                    .embedUrl(buildGoogleEmbedUrl(origin, destination, normalizedMode))
                    .shareUrl(buildGoogleShareUrl(origin, destination, normalizedMode))
                    .warnings(collectGoogleWarnings(directions, route))
                    .build();

            log.debug("Generated Google Maps route (mode={}, distance={}, duration={})",
                    normalizedMode, response.getDistanceText(), response.getDurationText());
            return response;
        } catch (IllegalStateException ex) {
            log.warn("Falling back to share-only Google route for origin='{}', destination='{}': {}",
                    origin, destination, ex.getMessage());
            return buildFallbackResponse(MapProvider.GOOGLE, origin, destination, normalizedMode, ex.getMessage());
        }
    }

    /**
     * AMAP route generation: geocode both sides via Google (WGS84) -> convert to GCJ-02 -> call AMap directions.
     * Falls back to AMap geocode if Google geocoding fails for either endpoint.
     * @param originLabel raw origin label (likely English)
     * @param destinationLabel raw destination label
     * @param normalizedMode validated travel mode
     * @return detailed AMap route or fallback response
     */
    private MapRouteResponse generateAmapRoute(String originLabel, String destinationLabel, String normalizedMode) {
        ensureAmapConfigured();
        try {
            // Step 1: Geocode via Google (WGS84)
            Optional<GoogleGeocodeResult> originGeo = geocodeViaGoogle(originLabel, null);
            Optional<GoogleGeocodeResult> destGeo = geocodeViaGoogle(destinationLabel, null);

            AmapLocation origin;
            AmapLocation destination;
            if (originGeo.isPresent()) {
                origin = buildAmapLocationFromGoogle(originLabel, originGeo.get(), false);
            } else {
                origin = resolveAmapLocation(originLabel);
            }
            if (destGeo.isPresent()) {
                destination = buildAmapLocationFromGoogle(destinationLabel, destGeo.get(), false);
            } else {
                destination = resolveAmapLocation(destinationLabel);
            }

            JsonNode payload = callAmapDirections(origin, destination, normalizedMode);
            JsonNode routeNode = payload.path("route");
            if (routeNode.isMissingNode()) {
                throw new IllegalStateException("AMap did not return a route");
            }

            JsonNode primary = extractPrimaryRouteNode(routeNode);
            if (primary == null) {
                throw new IllegalStateException("AMap did not return any paths for this request");
            }

            Integer distanceMeters = parseInteger(primary.path("distance").asText(null));
            Integer durationSeconds = parseInteger(primary.path("duration").asText(null));

            MapRouteResponse response = MapRouteResponse.builder()
                    .provider(MapProvider.AMAP)
                    .travelMode(normalizedMode)
                    .routeSummary(buildAmapSummary(origin, destination))
                    .distanceMeters(distanceMeters)
                    .distanceText(formatDistance(distanceMeters))
                    .durationSeconds(durationSeconds)
                    .durationText(formatDuration(durationSeconds))
                    .overviewPolyline(null)
                    .embedUrl(buildAmapEmbedUrl(origin, destination, normalizedMode))
                    .shareUrl(buildAmapShareUrl(origin, destination, normalizedMode))
                    .warnings(buildAmapWarnings(payload))
                    .build();

            log.debug("Generated AMap route (mode={}, distance={}, duration={})",
                    normalizedMode, response.getDistanceText(), response.getDurationText());
            return response;
        } catch (IllegalStateException ex) {
            log.warn("Falling back to share-only AMap route for origin='{}', destination='{}': {}",
                    originLabel, destinationLabel, ex.getMessage());
            return buildFallbackResponse(MapProvider.AMAP, originLabel, destinationLabel, normalizedMode, ex.getMessage());
        }
    }

    /**
     * Ensures Google Maps API key configured before any Google call.
     * @throws IllegalStateException if missing
     */
    private void ensureGoogleConfigured() {
        if (!StringUtils.hasText(mapsApiKey)) {
            throw new IllegalStateException("Google Maps API key is not configured");
        }
    }

    /**
     * Ensures AMap API key configured before any AMap call.
     * @throws IllegalStateException if missing
     */
    private void ensureAmapConfigured() {
        if (!StringUtils.hasText(amapServiceApiKey)) {
            throw new IllegalStateException("AMap API key is not configured");
        }
    }

    /**
     * Verifies travel mode is one of supported set.
     * @param normalizedMode lower-cased mode
     * @throws IllegalArgumentException if unsupported
     */
    private void validateModeSupported(String normalizedMode) {
        if (!SUPPORTED_MODES.contains(normalizedMode)) {
            throw new IllegalArgumentException("Unsupported travel mode: " + normalizedMode);
        }
    }

    /**
     * Normalizes and validates mode; defaults to driving when blank.
     * @param mode raw travel mode
     * @return normalized supported mode
     */
    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return DEFAULT_MODE;
        }
        String normalized = mode.trim().toLowerCase(Locale.ENGLISH);
        validateModeSupported(normalized);
        return normalized;
    }

    /**
     * Low-level call to Google Directions API; throws on non OK status or transport errors.
     * @param origin origin text/coordinate
     * @param destination destination text/coordinate
     * @param mode travel mode
     * @return deserialized GoogleDirectionsResponse
     */
    private GoogleDirectionsResponse callGoogleDirections(String origin, String destination, String mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(GOOGLE_DIRECTIONS_URL)
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

    /**
     * Builds embeddable Google Maps iframe URL.
     */
    private String buildGoogleEmbedUrl(String origin, String destination, String mode) {
        return UriComponentsBuilder.fromHttpUrl(GOOGLE_EMBED_BASE_URL)
                .queryParam("key", mapsApiKey)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("mode", mode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    /**
     * Builds share URL for Google Maps directions (no embed, direct link).
     */
    private String buildGoogleShareUrl(String origin, String destination, String mode) {
        return UriComponentsBuilder.fromHttpUrl(GOOGLE_SHARE_BASE_URL)
                .queryParam("api", 1)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("travelmode", mode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    /**
     * Collects distinct warning messages from top-level and route-level arrays.
     */
    private List<String> collectGoogleWarnings(GoogleDirectionsResponse directions,
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

    /**
     * Construct minimal fallback response when provider cannot yield detailed route.
     */
    private MapRouteResponse buildFallbackResponse(MapProvider provider,
                                                   String origin,
                                                   String destination,
                                                   String mode,
                                                   String reason) {
        List<String> warnings = new ArrayList<>();
        warnings.add(provider == MapProvider.GOOGLE
                ? "Google Maps Transit doesn't have any results in this area. Please click the share link to get more details."
                : "AMap could not provide a detailed route for this request. Please click the share link to get more details.");
        if (StringUtils.hasText(reason)) {
            warnings.add(reason);
        }
        String shareUrl = provider == MapProvider.GOOGLE
                ? buildGoogleShareUrl(origin, destination, mode)
                : buildAmapShareUrl(AmapLocation.fallback(origin), AmapLocation.fallback(destination), mode);
        String embedUrl = provider == MapProvider.GOOGLE ? null : shareUrl;
        return MapRouteResponse.builder()
                .provider(provider)
                .travelMode(mode)
                .embedUrl(embedUrl)
                .shareUrl(shareUrl)
                .warnings(warnings)
                .build();
    }

    /**
     * Geocode label via AMap when Google geocoding not used; returns coordinate + formatted address if available.
     */
    private AmapLocation resolveAmapLocation(String label) {
        if (isCoordinate(label)) {
            return AmapLocation.fromCoordinate(label, normalizeCoordinate(label));
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_GEOCODE_URL)
                .queryParam("address", label)
                .queryParam("key", amapServiceApiKey)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        try {
            // 向uri发起请求，把得到的response（json）格式化成dto
            AmapGeocodeResponse response = restTemplate.getForObject(uri, AmapGeocodeResponse.class);
            if (response == null) {
                throw new IllegalStateException("AMap geocoding returned empty response");
            }
            if (!"1".equals(response.getStatus())) {
                String message = Optional.ofNullable(response.getInfo()).orElse("AMap geocoding failed");
                throw new IllegalStateException(message);
            }
            AmapGeocode geocode = response.firstResult()
                    .orElseThrow(() -> new IllegalStateException("AMap could not geocode location: " + label));
            if (!StringUtils.hasText(geocode.getLocation())) {
                throw new IllegalStateException("AMap geocoding did not include coordinates");
            }
            return AmapLocation.fromGeocode(label, geocode);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("AMap geocoding error: status={}, body={}", ex.getRawStatusCode(), body);
            throw new IllegalStateException("Failed to geocode location via AMap: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("AMap geocoding invocation failed", ex);
            throw new IllegalStateException("Unable to reach AMap geocoding API", ex);
        }
    }

    /**
     * Quick coordinate string format check (lon,lat or lat,lon both treated generically here).
     */
    private boolean isCoordinate(String label) {
        return StringUtils.hasText(label) && COORDINATE_PATTERN.matcher(label).matches();
    }

    /**
     * Removes extra whitespace around coordinate components.
     */
    private String normalizeCoordinate(String value) {
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return value.trim();
        }
        return parts[0].trim() + "," + parts[1].trim();
    }

    /**
     * Calls appropriate AMap direction endpoint based on travel mode using GCJ-02 coordinates.
     */
    private JsonNode callAmapDirections(AmapLocation origin, AmapLocation destination, String normalizedMode) {
        if (!origin.hasCoordinate() || !destination.hasCoordinate()) {
            throw new IllegalStateException("AMap requires latitude/longitude pairs to generate routes");
        }
        UriComponentsBuilder builder;
        switch (normalizedMode) {
            case "walking" -> builder = UriComponentsBuilder.fromHttpUrl(AMAP_WALKING_URL);
            case "bicycling" -> builder = UriComponentsBuilder.fromHttpUrl(AMAP_BICYCLING_URL);
            case "transit" -> {
                builder = UriComponentsBuilder.fromHttpUrl(AMAP_TRANSIT_URL);
                String city = StringUtils.hasText(destination.citycode()) ? destination.citycode() : origin.citycode();
                if (StringUtils.hasText(city)) {
                    builder.queryParam("city", city);
                }
            }
            default -> builder = UriComponentsBuilder.fromHttpUrl(AMAP_DRIVING_URL)
                    .queryParam("extensions", "base");
        }
        builder.queryParam("origin", origin.coordinate())
                .queryParam("destination", destination.coordinate())
                .queryParam("key", amapServiceApiKey);
        URI uri = builder.build().encode(StandardCharsets.UTF_8).toUri();
        return executeAmapRequest(uri);
    }

    /**
     * Executes AMap HTTP request, validates status and returns parsed JSON tree.
     */
    private JsonNode executeAmapRequest(URI uri) {
        try {
            String response = restTemplate.getForObject(uri, String.class);
            if (!StringUtils.hasText(response)) {
                throw new IllegalStateException("Empty response from AMap");
            }
            JsonNode node = objectMapper.readTree(response);
            if (!"1".equals(node.path("status").asText())) {
                String info = node.path("info").asText("AMap returned an error");
                throw new IllegalStateException(info);
            }
            return node;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("AMap API error: status={}, body={}", ex.getRawStatusCode(), body);
            throw new IllegalStateException("Failed to reach AMap API: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("AMap API invocation failed", ex);
            throw new IllegalStateException("Unable to call AMap API", ex);
        } catch (IOException ex) {
            log.error("Failed to parse AMap response", ex);
            throw new IllegalStateException("Unable to parse AMap response payload", ex);
        }
    }

    /**
     * Picks first route node array among candidate field names (paths / transits).
     */
    private JsonNode extractPrimaryRouteNode(JsonNode routeNode) {
        List<String> candidates = List.of("paths", "transits");
        for (String field : candidates) {
            JsonNode array = routeNode.path(field);
            if (array.isArray() && array.size() > 0) {
                return array.get(0);
            }
        }
        return null;
    }

    /**
     * Creates summary string for AMap route.
     */
    private String buildAmapSummary(AmapLocation origin, AmapLocation destination) {
        return origin.displayName() + " → " + destination.displayName();
    }

    /**
     * Builds list of warning messages for AMap response.
     */
    private List<String> buildAmapWarnings(JsonNode payload) {
        List<String> warnings = new ArrayList<>();
        String info = payload.path("info").asText();
        if (StringUtils.hasText(info) && !"OK".equalsIgnoreCase(info)) {
            warnings.add(info);
        }
        warnings.add("Route data powered by AMap (Gaode).");
        return warnings;
    }

    /**
     * AMap embed URL currently same as share URL (no specialized embed endpoint used).
     */
    private String buildAmapEmbedUrl(AmapLocation origin, AmapLocation destination, String normalizedMode) {
        return buildAmapShareUrl(origin, destination, normalizedMode);
    }

    /**
     * Builds AMap share/navigation URL with waypoints and travel mode.
     */
    private String buildAmapShareUrl(AmapLocation origin, AmapLocation destination, String normalizedMode) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(AMAP_SHARE_BASE_URL)
                .queryParam("callnative", 0)
                .queryParam("mode", amapModeParam(normalizedMode))
                .queryParam("coordinate", "gaode"); // explicit coordinate system to avoid certain redirects/login walls
        builder.queryParam("from", buildAmapWaypoint(origin));
        builder.queryParam("to", buildAmapWaypoint(destination));
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    /**
     * Waypoint builder adds label to coordinate when available for friendlier display.
     */
    private String buildAmapWaypoint(AmapLocation location) {
        if (location == null) {
            return "";
        }
        String label = location.displayName();
        if (location.hasCoordinate()) {
            return location.coordinate() + "," + label;
        }
        return label;
    }

    /**
     * Maps generic normalized mode to AMap specific mode parameter.
     */
    private String amapModeParam(String normalizedMode) {
        return switch (normalizedMode) {
            case "walking" -> "walk";
            case "bicycling" -> "ride";
            case "transit" -> "bus";
            default -> "car";
        };
    }

    /**
     * Parses numeric string to Integer rounding if decimal; returns null on failure.
     */
    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            double doubleValue = Double.parseDouble(value.trim());
            return (int) Math.round(doubleValue);
        } catch (NumberFormatException ex) {
            log.debug("Unable to parse integer from value '{}'", value);
            return null;
        }
    }

    /**
     * Human-readable distance formatting with km/m thresholds.
     */
    private String formatDistance(Integer meters) {
        if (meters == null) {
            return null;
        }
        if (meters >= 1000) {
            double km = meters / 1000d;
            if (km >= 10) {
                return String.format(Locale.ENGLISH, "%d km", Math.round(km));
            }
            return String.format(Locale.ENGLISH, "%.1f km", km);
        }
        return meters + " m";
    }

    /**
     * Human-readable duration formatting (hours/minutes).
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        if (hours > 0) {
            return minutes > 0
                    ? String.format(Locale.ENGLISH, "%d hr %d min", hours, minutes)
                    : String.format(Locale.ENGLISH, "%d hr", hours);
        }
        return String.format(Locale.ENGLISH, "%d min", Math.max(1, minutes));
    }

    /**
     * Attempts Google geocoding for a label (optional language); returns first result or empty.
     */
    private Optional<GoogleGeocodeResult> geocodeViaGoogle(String label, String language) {
        if (!StringUtils.hasText(label) || !StringUtils.hasText(mapsApiKey)) {
            return Optional.empty();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(GOOGLE_GEOCODE_URL)
                .queryParam("address", label)
                .queryParam("key", mapsApiKey)
                .queryParamIfPresent("language", Optional.ofNullable(language))
                .build().encode(StandardCharsets.UTF_8).toUri();
        try {
            // 尝试 Google；无 key 或失败都返回 Optional.empty()
            // 向uri发起请求，把得到的response（json）格式化成dto
            GoogleGeocodeResponse response = restTemplate.getForObject(uri, GoogleGeocodeResponse.class);
            if (response == null || !"OK".equalsIgnoreCase(response.getStatus())) return Optional.empty();
            return response.firstResult();
        } catch (Exception ex) {
            log.debug("Google geocoding skipped for '{}': {}", label, ex.toString());
            return Optional.empty();
        }
    }


    /**
     * Converts Google WGS84 location to GCJ-02 (if inside China) and wraps in AmapLocation.
     */
    private AmapLocation buildAmapLocationFromGoogle(String originalLabel, GoogleGeocodeResult result, boolean preferChinese) {
        double wgsLat = result.getGeometry().getLocation().getLat();
        double wgsLng = result.getGeometry().getLocation().getLng();
        double[] gcj = wgs84ToGcj02(wgsLat, wgsLng);
        String coordinate = formatCoordinate(gcj[1], gcj[0]); // lon,lat for AMap
        String display = StringUtils.hasText(result.getFormattedAddress()) ? result.getFormattedAddress() : originalLabel;
        return AmapLocation.fromCoordinate(originalLabel, coordinate, display);
    }

    /**
     * Formats longitude/latitude to 6 decimal places for AMap.
     */
    private String formatCoordinate(double lon, double lat) {
        return round6(lon) + "," + round6(lat);
    }

    /**
     * Rounds double to 6 decimals (half-up) and strips trailing zeros.
     */
    private String round6(double v) {
        return BigDecimal.valueOf(v).setScale(6, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * WGS84 -> GCJ-02 conversion; returns original coords if outside China region bounds.
     */
    private double[] wgs84ToGcj02(double lat, double lon) {
        if (outOfChina(lat, lon)) return new double[]{lat, lon};
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - 0.00669342162296594323 * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((6335552.717000426 * magic) / (sqrtMagic * sqrtMagic) * Math.PI);
        dLon = (dLon * 180.0) / (6378245.0 / sqrtMagic * Math.cos(radLat) * Math.PI);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }

    /**
     * Bounding box check for China to decide whether coordinate transform is applied.
     */
    private boolean outOfChina(double lat, double lon) {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    /**
     * Internal latitude delta calculation for GCJ-02 transform.
     */
    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * Internal longitude delta calculation for GCJ-02 transform.
     */
    private double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
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

    // ----------- Internal DTOs for AMap responses -----------

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmapGeocodeResponse {
        private String status;
        private String info;
        private List<AmapGeocode> geocodes;

        Optional<AmapGeocode> firstResult() {
            return Optional.ofNullable(geocodes)
                    .stream()
                    .flatMap(List::stream)
                    .findFirst();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmapGeocode {
        @JsonProperty("formatted_address")
        private String formattedAddress;
        private String location;
        private String citycode;
        private String adcode;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoogleGeocodeResponse {
        private String status;
        private List<GoogleGeocodeResult> results;
        Optional<GoogleGeocodeResult> firstResult() {
            return Optional.ofNullable(results).stream().flatMap(List::stream).findFirst();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoogleGeocodeResult {
        private String formatted_address;
        private Geometry geometry;
        String getFormattedAddress() { return formatted_address; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Geometry {
        private Location location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Location {
        private double lat;
        private double lng;
    }

    private record AmapLocation(String originalLabel,
                                String coordinate,
                                String citycode,
                                String adcode,
                                String displayName) {

        static AmapLocation fromCoordinate(String label, String coordinate) {
            return new AmapLocation(label, coordinate, null, null, label);
        }
        static AmapLocation fromCoordinate(String label, String coordinate, String display) {
            return new AmapLocation(label, coordinate, null, null, display);
        }

        static AmapLocation fromGeocode(String label, AmapGeocode geocode) {
            String display = StringUtils.hasText(geocode.getFormattedAddress())
                    ? geocode.getFormattedAddress()
                    : label;
            String coords = geocode.getLocation() != null ? geocode.getLocation().trim() : null;
            return new AmapLocation(label, coords, geocode.getCitycode(), geocode.getAdcode(), display);
        }

        static AmapLocation fallback(String label) {
            return new AmapLocation(label, null, null, null, label);
        }

        boolean hasCoordinate() {
            return StringUtils.hasText(coordinate);
        }
    }
}
