package com.demo.api.dto.map;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Response payload returned for a generated Google Maps route.
 */
@Data
@Builder
public class MapRouteResponse {

    /** Travel mode requested (normalized). */
    private String travelMode;

    /** Compact summary returned by Google Directions API. */
    private String routeSummary;

    /** Human-readable distance for the first leg, e.g. "10.5 km". */
    private String distanceText;

    /** Distance in meters for the first leg. */
    private Integer distanceMeters;

    /** Human-readable duration for the first leg, e.g. "15 mins". */
    private String durationText;

    /** Duration in seconds for the first leg. */
    private Integer durationSeconds;

    /** Encoded polyline points for drawing the path client-side. */
    private String overviewPolyline;

    /** IFrame-friendly embed URL (uses Google Maps Embed API). */
    private String embedUrl;

    /** Shareable link to open the same route in Google Maps. */
    private String shareUrl;

    /** Any warnings returned by Google Directions API. */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}

