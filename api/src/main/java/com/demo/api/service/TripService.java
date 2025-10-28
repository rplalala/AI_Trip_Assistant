package com.demo.api.service;

import com.demo.api.dto.TimeLineDTO;
import com.demo.api.dto.TripDetailDTO;

import java.util.List;

public interface TripService {
    List<TripDetailDTO> getTripDetails(Long userId);

    void deleteTripByIds(List<Long> tripIds);

    List<TimeLineDTO> getTimeLine(Long tripId);
}
