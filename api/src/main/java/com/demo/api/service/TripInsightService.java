package com.demo.api.service;

import com.demo.api.dto.TripInsightDTO;
import com.demo.api.model.TripInsight;

import java.util.List;

public interface TripInsightService {
    List<TripInsight> generateAndStoreInsights(Long tripId);

    List<TripInsightDTO> getOrGenerateInsights(Long tripId);
}
