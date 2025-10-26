package com.demo.api.service;

import com.demo.api.dto.TripDetailDTO;

import java.util.List;

public interface TripService {
    List<TripDetailDTO> getTripDetails(Long userId);
}
