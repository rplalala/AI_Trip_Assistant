package com.demo.api.service.prompt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.demo.api.dto.DailyWeatherDTO;
import com.demo.api.model.TripPreference;

@Component
public class PromptBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    /**
     * Builds a natural language prompt that can be sent to a GPT-style model in order to
     * generate a detailed trip plan.
     *
     * @param preference the user's trip preference data
     * @param weatherList optional list of daily weather summaries for the trip window
     * @return formatted prompt string
     */
    public String build(TripPreference preference, List<DailyWeatherDTO> weatherList) {
        Assert.notNull(preference, "Trip preference must not be null");

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are an expert travel planner. Use the provided data to craft a comprehensive trip itinerary.
                
                Trip Overview:
                """);

        appendTripOverview(prompt, preference);
        appendPreferences(prompt, preference);
        appendWeather(prompt, weatherList);
        appendInstructions(prompt);

        return prompt.toString();
    }

    private void appendTripOverview(StringBuilder prompt, TripPreference preference) {
        String city = defaultString(preference.getCity(), "Unknown city");
        String country = defaultString(preference.getCountry(), "Unknown country");
        LocalDate startDate = preference.getStartDate();
        LocalDate endDate = preference.getEndDate();
        Long duration = calculateDuration(startDate, endDate);

        prompt.append(String.format("- Destination: %s, %s%n", city, country));
        if (startDate != null && endDate != null) {
            prompt.append(String.format("- Travel dates: %s to %s",
                    startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER)));
            if (duration != null) {
                prompt.append(String.format(" (%d days)%n", duration));
            } else {
                prompt.append('\n');
            }
        } else if (startDate != null) {
            prompt.append(String.format("- Travel start date: %s%n", startDate.format(DATE_FORMATTER)));
        } else {
            prompt.append("- Travel dates: Not specified\n");
        }

        String currency = defaultString(preference.getCurrency(), "AUD");
        Integer budget = preference.getBudget();
        prompt.append(String.format("- Budget: %s%n", budget != null ? budget + " " + currency : "Not specified"));

        Integer people = preference.getPeople();
        prompt.append(String.format("- Travelers: %s%n", people != null ? people + " people" : "Not specified"));
    }

    private void appendPreferences(StringBuilder prompt, TripPreference preference) {
        prompt.append("\nTraveler Preferences:\n");
        prompt.append(String.format("- Notes: %s%n", defaultString(preference.getPreferences(), "No additional preferences")));
    }

    private void appendWeather(StringBuilder prompt, List<DailyWeatherDTO> weatherList) {
        if (CollectionUtils.isEmpty(weatherList)) {
            return;
        }

        prompt.append("\nWeather Forecast (local time):\n");
        weatherList.stream()
                .sorted((w1, w2) -> {
                    LocalDate d1 = w1.getDate();
                    LocalDate d2 = w2.getDate();
                    if (d1 == null && d2 == null) {
                        return 0;
                    }
                    if (d1 == null) {
                        return 1;
                    }
                    if (d2 == null) {
                        return -1;
                    }
                    return d1.compareTo(d2);
                })
                .forEach(weather -> {
                    String date = weather.getDate() != null ? weather.getDate().format(DATE_FORMATTER) : "Unknown date";
                    String minTemp = weather.getMinTemp() != null ? String.format(Locale.ENGLISH, "%.1f°C", weather.getMinTemp()) : "N/A";
                    String maxTemp = weather.getMaxTemp() != null ? String.format(Locale.ENGLISH, "%.1f°C", weather.getMaxTemp()) : "N/A";
                    String condition = defaultString(weather.getWeatherCondition(), "Condition unavailable");

                    prompt.append(String.format("- %s: min %s / max %s, %s%n", date, minTemp, maxTemp, condition));
                });
    }

    private void appendInstructions(StringBuilder prompt) {
        prompt.append("""
                    
                    Instructions for the itinerary generation:
    
                    - Return ONLY a JSON object with **two keys**: "daily_summaries" and "activities".
    
                    - "daily_summaries" is a list of objects, each containing (in order):
                        - "date": in yyyy-MM-dd format
                        - "summary": a short description of the day's main theme or focus
    
                    - "activities" is a list of scheduled trip activities with the following possible types:
                        - transportation
                        - hotel
                        - attraction
    
                    - Each activity must include (in order):
                        - "date": in yyyy-MM-dd format
                        - "type": one of the three types
                        - "time": time of the activity (e.g., "15:00")
                        - "title": short name of the activity
                        - "people": number of travelers
                        - "currency": e.g., "AUD" or "JPY"
                        - "reservation_required": true/false
    
                    - For "transportation" activities, also include (in order):
                        - "from", "to", "transport_mode", "provider", "ticket_type", "price"
    
                    - For "hotel" activities, also include (in order):
                        - "hotel_name", "room_type", "nights", "price"
    
                    - For "attraction" activities, also include (in order):
                        - "location", "ticket_price"
    
                    - Only include fields relevant to the activity type.
    
                    - Make sure all currency values match the trip's original currency, and that total cost stays within the budget.
    
                    - Return a clean JSON object with no additional text or markdown.
                    """);
    }
    private Long calculateDuration(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return null;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private String defaultString(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}