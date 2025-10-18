package com.demo.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.service.TripGenerationService;

@RestController
@RequestMapping("/api/trip")
public class TripTestController {

    private static final Logger log = LoggerFactory.getLogger(TripTestController.class);

    private final TripGenerationService tripGenerationService;

    public TripTestController(TripGenerationService tripGenerationService) {
        this.tripGenerationService = tripGenerationService;
    }

    @PostMapping("/test-generate")
    public ResponseEntity<String> testGenerate(@RequestBody TripPreferenceRequestDTO dto) {
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
        return ResponseEntity.ok(generatedJson);
    }
}

