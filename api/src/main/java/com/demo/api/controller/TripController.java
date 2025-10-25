package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.TripInsightDTO;
import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.service.TripGenerationService;
import com.demo.api.service.TripInsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip")
public class TripController {

    private static final Logger log = LoggerFactory.getLogger(TripController.class);

    private final TripGenerationService tripGenerationService;

    private final TripInsightService tripInsightService;

    public TripController(TripGenerationService tripGenerationService, TripInsightService tripInsightService) {
        this.tripGenerationService = tripGenerationService;
        this.tripInsightService = tripInsightService;
    }

    @PostMapping("/generate-plan")
    public ApiRespond<Void> generatePlan(@RequestBody TripPreferenceRequestDTO dto,
                                         @AuthenticationPrincipal String userId) {

        log.info("Received trip generation request: {}", dto);

        tripGenerationService.generateTripAndReturnJson(dto, userId);

        // return the generated JSON as the response body
        return ApiRespond.success();
    }

    @GetMapping("/insights")
    public ApiRespond<List<TripInsightDTO>> getInsights(@RequestParam("tripId") Long tripId) {
        return ApiRespond.success(tripInsightService.getOrGenerateInsights(tripId));
    }
}

