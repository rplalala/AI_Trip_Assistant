package com.demo.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ItineraryDTO {

    private List<DailySummaryDTO> dailySummaries;
    private List<ActivityDTO> activities;

    // ------------ Daily Summary ------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class DailySummaryDTO {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate date;
        private String summary;
        private String imageUrl;
        private String imageDescription;
    }

    // ------------ Activities (polymorphic) ------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type",
            visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TransportationDTO.class, name = "transportation"),
            @JsonSubTypes.Type(value = HotelDTO.class,           name = "hotel"),
            @JsonSubTypes.Type(value = AttractionDTO.class,      name = "attraction")
    })
    public static class ActivityDTO {
        private String type;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate date;
        private String time;
        private String title;
        private String status;
        private Boolean reservationRequired;
        private String imageUrl;
        private String imageDescription;
    }

    // ------------ transportation ------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TransportationDTO extends ActivityDTO {
        private String from;
        private String to;
        private String provider;
        private String ticketType;
        private Integer price;
        private String currency;
        private String imageDescription;
    }

    // ------------ hotel ------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class HotelDTO extends ActivityDTO {
        private String hotelName;
        private String roomType;
        private Integer people;
        private Integer nights;
        private Integer price;
        private String currency;
        private String imageDescription;
    }

    // ------------ attraction ------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AttractionDTO extends ActivityDTO {
        private String location;
        private Integer ticketPrice;
        private Integer people;
        private String currency;
        private String imageDescription;
    }
}
