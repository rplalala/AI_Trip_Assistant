package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.MapRouteRequest;
import com.demo.api.dto.MapRouteResponse;
import com.demo.api.enums.MapProvider;
import com.demo.api.service.MapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes endpoints for generating Google Maps routes.
 */
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@Validated
public class MapController {

    private final MapService mapService;

    /**
     * Generates a map route between origin and destination using Google Directions API or AMap Web API.
     *
     * @param request request payload with origin, destination, and optional travel mode
     * @return route metadata including embed and share URLs
     */
    @PostMapping("/route")
    public ApiRespond<MapRouteResponse> generateRoute(@Valid @RequestBody MapRouteRequest request) {
        // 根据前端传入的Map服务商指定后端调用的Map服务商
        MapProvider provider = mapService.mapProviderSelect(request.getProvider());
        return ApiRespond.success(mapService.generateRoute(request, provider));
    }
}

