package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.TimeLineDTO;
import com.demo.api.dto.TripDetailDTO;
import com.demo.api.dto.TripInsightDTO;
import com.demo.api.dto.TripPreferenceRequestDTO;
import com.demo.api.service.TripGenerationService;
import com.demo.api.service.TripInsightService;
import com.demo.api.service.TripService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip")
@RequiredArgsConstructor
public class TripController {

    private static final Logger log = LoggerFactory.getLogger(TripController.class);

    private final TripGenerationService tripGenerationService;

    private final TripInsightService tripInsightService;

    private final TripService tripServiceImpl;

    @PostMapping("/generate-plan")
    public ApiRespond<Void> generatePlan(@RequestBody TripPreferenceRequestDTO dto,
                                         @AuthenticationPrincipal String userId) {

        log.info("Received trip generation request: {}", dto);

        tripGenerationService.generateTripAndReturnJson(dto, userId);

        // return the generated JSON as the response body
        return ApiRespond.success();
    }

    /**
     * Get the all trips details for the authenticated user
     * @param userId
     * @return
     */
    @GetMapping("/details")
    public ApiRespond<List<TripDetailDTO>> getTripDetails(@AuthenticationPrincipal String userId){
        log.info("Current User: {}", userId);
        return ApiRespond.success(tripServiceImpl.getTripDetails(Long.valueOf(userId)));
    }

    @GetMapping("/insights")
    public ApiRespond<List<TripInsightDTO>> getInsights(@RequestParam("tripId") Long tripId) {
        return ApiRespond.success(tripInsightService.getOrGenerateInsights(tripId));
    }

    /**
     * Get the trip timeline
     * @param tripId
     * @return
     */
    @GetMapping("/timeline")
    public ApiRespond<List<TimeLineDTO>> getTimeline(@RequestParam("tripId") Long tripId) {
        return ApiRespond.success(tripServiceImpl.getTimeLine(tripId));
    }
    /**
     * Delete trips in batch
     * @param tripIds
     * @return
     */
    @DeleteMapping
    public ApiRespond<Void> deleteTrips(@RequestParam("tripIds") List<Long> tripIds){
        tripServiceImpl.deleteTripByIds(tripIds);
        return ApiRespond.success();
    }
}

