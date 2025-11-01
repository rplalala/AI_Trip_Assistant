package com.demo.api.service.prompt;

import com.demo.api.dto.DailyWeatherDTO;
import com.demo.api.dto.ModifyPlanDTO;
import com.demo.api.model.Trip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripPlanPromptBuilderTest {

    private final TripPlanPromptBuilder builder = new TripPlanPromptBuilder();

    @DisplayName("build assembles overview, preferences, weather, and instructions")
    @Test
    void build_includesAllSections() {
        Trip trip = Trip.builder()
                .fromCity("Sydney")
                .fromCountry("Australia")
                .toCity("Tokyo")
                .toCountry("Japan")
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 3))
                .people(2)
                .budget(5000)
                .currency("AUD")
                .preferences("Prefer local cuisine and cultural experiences")
                .build();

        List<DailyWeatherDTO> weather = List.of(
                DailyWeatherDTO.builder()
                        .date(LocalDate.of(2025, 11, 1))
                        .minTemp(12.5)
                        .maxTemp(19.2)
                        .weatherCondition("Partly Cloudy")
                        .build(),
                DailyWeatherDTO.builder()
                        .date(LocalDate.of(2025, 11, 2))
                        .minTemp(10.1)
                        .maxTemp(20.0)
                        .weatherCondition("Light Rain")
                        .build()
        );

        String prompt = builder.build(trip, weather);

        assertThat(prompt)
                .contains("Departure: Sydney, Australia")
                .contains("Destination: Tokyo, Japan")
                .contains("Travel dates: 2025-11-01 to 2025-11-03 (3 days)")
                .contains("Budget: 5000 AUD")
                .contains("Prefer local cuisine and cultural experiences")
                .contains("Prefer local cuisine")
                .contains("Light Rain")
                .contains("\"daily_summaries\"")
                .contains("\"activities\"")
                .contains("Do NOT return markdown");
    }

    @DisplayName("buildForRegeneration emphasises strict preferences and existing notes")
    @Test
    void buildForRegeneration_highlightsStrictPreferences() {
        Trip trip = Trip.builder()
                .fromCity("Melbourne")
                .toCity("Seoul")
                .startDate(LocalDate.of(2025, 4, 10))
                .endDate(LocalDate.of(2025, 4, 12))
                .preferences("Museum visits preferred")
                .build();

        ModifyPlanDTO modifyPlanDTO = new ModifyPlanDTO();
        modifyPlanDTO.setSecondPreference("Must include at least one Michelin starred restaurant");

        String prompt = builder.buildForRegeneration(trip, List.of(), modifyPlanDTO);

        assertThat(prompt)
                .contains("Regenerate the itinerary")
                .contains("Museum visits preferred")
                .contains("STRICT PREFERENCES")
                .contains("Michelin starred restaurant");
    }

    @DisplayName("build tolerates missing travel dates and empty weather")
    @Test
    void build_handlesMissingData() {
        Trip trip = Trip.builder()
                .fromCity(null)
                .toCity("Perth")
                .endDate(LocalDate.of(2025, 5, 5))
                .build();

        String prompt = builder.build(trip, List.of());

        assertThat(prompt)
                .contains("Departure: Unknown city")
                .contains("Travel dates: Not specified")
                .doesNotContain("Weather Forecast");
    }
    @DisplayName("build handles invalid durations and incomplete weather data")
    @Test
    void build_handlesNullWeatherValues() {
        Trip trip = Trip.builder()
                .fromCity("Brisbane")
                .fromCountry("Australia")
                .toCity("Queenstown")
                .toCountry("New Zealand")
                .startDate(LocalDate.of(2025, 6, 10))
                .endDate(LocalDate.of(2025, 6, 8)) // end before start -> duration null
                .budget(null)
                .people(null)
                .currency(null)
                .preferences("   ") // blank triggers fallback
                .build();

        List<DailyWeatherDTO> weather = List.of(
                DailyWeatherDTO.builder()
                        .date(null)
                        .minTemp(null)
                        .maxTemp(null)
                        .weatherCondition(null)
                        .build(),
                DailyWeatherDTO.builder()
                        .date(LocalDate.of(2025, 6, 9))
                        .minTemp(3.0)
                        .maxTemp(11.0)
                        .weatherCondition("Snow")
                        .build()
        );

        String prompt = builder.build(trip, weather);

        assertThat(prompt)
                .contains("Travel dates: 2025-06-10 to 2025-06-08")
                .contains("Budget: Not specified")
                .contains("Travelers: Not specified")
                .contains("- Unknown date: min N/A / max N/A, Condition unavailable")
                .contains("- 2025-06-09: min 3.0°C / max 11.0°C, Snow");
    }
}
