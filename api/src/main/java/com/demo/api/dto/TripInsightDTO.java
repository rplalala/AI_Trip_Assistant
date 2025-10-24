package com.demo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripInsightDTO {
    private String id;
    private String title;
    private String content;
    private String theme;
    private String icon;
}
