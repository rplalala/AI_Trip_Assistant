package com.demo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripDetailDTO {
    private Long tripId;
    private String fromCountry;
    private String fromCity;
    private String toCountry;
    private String toCity;
    private Integer budget;
    private Integer people;
    private LocalDate startDate;
    private LocalDate endDate;
    private String imgUrl;
}
