package com.demo.api.service;

import com.demo.api.dto.MapRouteRequest;
import com.demo.api.dto.MapRouteResponse;
import com.demo.api.enums.MapProvider;

public interface MapService {

    /**
     * Generates a Google Maps route based on the provided origin/destination.
     *
     * @param request request payload with origin, destination, and optional travel mode
     * @param provider which provider should satisfy the request
     * @return generated route details including embed/share URLs
     */
    MapRouteResponse generateRoute(MapRouteRequest request, MapProvider provider);
    MapProvider mapProviderSelect(String requestedProvider);
}

