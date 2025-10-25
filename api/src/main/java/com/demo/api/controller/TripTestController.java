package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.TripInsightDTO;
import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.service.TripGenerationService;
import com.demo.api.service.TripInsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip")
public class TripTestController {

    private static final Logger log = LoggerFactory.getLogger(TripTestController.class);

    private final TripGenerationService tripGenerationService;

    private final TripInsightService tripInsightService;

    public TripTestController(TripGenerationService tripGenerationService, TripInsightService tripInsightService) {
        this.tripGenerationService = tripGenerationService;
        this.tripInsightService = tripInsightService;
    }

    @PostMapping("/test-generate")
    public ApiRespond<String> testGenerate(@RequestBody TripPreferenceRequestDTO dto) {
        log.info("Received test trip generation request for user {} from {}, {} to {}, {}",
                dto.getUserId(),
                dto.getFromCity(),
                dto.getFromCountry(),
                dto.getToCity(),
                dto.getToCountry());

        tripGenerationService.generateTripAndReturnJson(dto);

        String generatedJson = tripGenerationService.generateTripAndReturnJson(dto);

        log.info("Generated Trip JSON:\n{}", generatedJson);

        // return the generated JSON as the response body
        return ApiRespond.success(generatedJson);
    }

    @GetMapping("/insights")
    public ApiRespond<List<TripInsightDTO>> getInsights(@RequestParam("tripId") Long tripId) {
        return ApiRespond.success(tripInsightService.getOrGenerateInsights(tripId));
    }
}

