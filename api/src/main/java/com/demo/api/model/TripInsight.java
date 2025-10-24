package com.demo.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "trip_insights",
        schema = "public",
        indexes = {
                @Index(name = "idx_trip_insight_trip_id", columnList = "trip_id"),
                @Index(name = "idx_trip_insight_theme", columnList = "theme")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_insight_title_per_trip", columnNames = {"trip_id", "title"})
        }
)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripInsight extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 30)
    private String theme; // history, food, etc.

    @Column(length = 8)
    private String icon; // emoji
}
