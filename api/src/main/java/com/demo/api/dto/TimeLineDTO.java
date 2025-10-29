package com.demo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeLineDTO {
    private String date;
    private String imageUrl;
    private String summary;
    private Double maxTemperature;
    private Double minTemperature;
    private String weatherCondition;
    private List<AttractionTimeLineDTO> attraction;
    private List<HotelTimeLineDTO> hotel;
    private List<TransportationTimeLineDTO> transportation;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AttractionTimeLineDTO {
        private String location;
        private String time;
        private String title;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class HotelTimeLineDTO {
        private String hotelName;
        private String time;
        private String title;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TransportationTimeLineDTO {
        private String time;
        private String title;
        private String from;
        private String to;
    }
}
