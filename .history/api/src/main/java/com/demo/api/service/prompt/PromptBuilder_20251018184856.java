package com.demo.api.service.prompt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.demo.api.dto.DailyWeatherDTO;
import com.demo.api.model.TripPreference;

/**
 * Builds the natural language prompt sent to the GPT model.
 */
@Component
public class PromptBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    public String build(TripPreference preference, List<DailyWeatherDTO> weatherSummaries) {
        Assert.notNull(preference, "Trip preference must not be null");

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert travel planner. ");
        prompt.append("Create a multi-day itinerary based on the user's preferences.\n\n");

        prompt.append("Trip overview:\n");
        prompt.append("- Traveler userId: ").append(preference.getUserId()).append('\n');
        prompt.append("- Route: ")
                .append(defaultText(preference.getFromCity(), "Unknown city"))
                .append(", ")
                .append(defaultText(preference.getFromCountry(), "Unknown country"))
                .append(" -> ")
                .append(defaultText(preference.getToCity(), "Unknown city"))
                .append(", ")
                .append(defaultText(preference.getToCountry(), "Unknown country"))
                .append('\n');

        appendDate(prompt, "Start date", preference.getStartDate());
        appendDate(prompt, "End date", preference.getEndDate());

        prompt.append("- Budget: ")
                .append(preference.getBudget() != null ? preference.getBudget() : "Not specified")
                .append(' ')
                .append(defaultText(preference.getCurrency(), "AUD"))
                .append('\n');

        prompt.append("- Travellers: ").append(preference.getPeople() != null ? preference.getPeople() : "Not specified").append('\n');
        prompt.append("- Preferences: ").append(defaultText(preference.getPreferences(), "No additional notes")).append("\n\n");

        if (!CollectionUtils.isEmpty(weatherSummaries)) {
            prompt.append("Weather forecast (daily):\n");
            weatherSummaries.forEach(summary -> {
                String minTemp = summary.getMinTemp() != null
                        ? String.format(Locale.ENGLISH, "%.1f°C", summary.getMinTemp())
                        : "N/A";
                String maxTemp = summary.getMaxTemp() != null
                        ? String.format(Locale.ENGLISH, "%.1f°C", summary.getMaxTemp())
                        : "N/A";
                prompt.append(String.format(Locale.ENGLISH,
                        "- %s: min %s, max %s, condition %s%n",
                        formatDate(summary.getDate()),
                        minTemp,
                        maxTemp,
                        defaultText(summary.getWeatherCondition(), "Unknown")));
            });
            prompt.append('\n');
        }

        prompt.append("Return a JSON payload describing daily summaries and detailed activities. ");
        prompt.append("Include transportation, lodging, and attraction recommendations with timing details.");

        return prompt.toString();
    }

    private void appendDate(StringBuilder prompt, String label, LocalDate date) {
        prompt.append("- ").append(label).append(": ").append(formatDate(date)).append('\n');
    }

    private String formatDate(LocalDate date) {
        return date != null ? DATE_FORMAT.format(date) : "Not specified";
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
