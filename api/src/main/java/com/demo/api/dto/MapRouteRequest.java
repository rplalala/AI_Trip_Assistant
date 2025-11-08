package com.demo.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for generating a Google Maps route.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapRouteRequest {

    /**
     * Origin location. Accepts free-form address or "lat,lng".
     */
    @NotBlank(message = "origin is required")
    private String origin;

    /**
     * Destination location. Accepts free-form address or "lat,lng".
     */
    @NotBlank(message = "destination is required")
    private String destination;

    /**
     * Travel mode supported by Google/AMap APIs (driving, walking, bicycling, transit).
     * Defaults to driving when omitted.
     */
    private String travelMode;

    /**
     * Optional map provider override supplied by the frontend (e.g. "google", "amap").
     * When blank the backend will infer the best provider based on the user's region.
     */
    private String provider;
}

