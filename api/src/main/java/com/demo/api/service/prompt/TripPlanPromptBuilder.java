package com.demo.api.service.prompt;

import com.demo.api.dto.DailyWeatherDTO;
import com.demo.api.dto.ModifyPlanDTO;
import com.demo.api.model.Trip;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class TripPlanPromptBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    /**
     * Builds a natural language prompt that can be sent to a GPT-style model in order to
     * generate a detailed trip plan.
     *
     * @param preference the user's trip preference data
     * @param weatherList optional list of daily weather summaries for the trip window
     * @return formatted prompt string
     */
    public String build(Trip preference, List<DailyWeatherDTO> weatherList) {
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
        log.info("generated prompt to ai:{}", prompt);
        return prompt.toString();
    }

    /**
     * Builds a stricter prompt for regeneration, and based on the previous trip information.
     * @param trip
     * @param weatherList
     * @param modifyPlanDTO
     * @return
     */
    public String buildForRegeneration(Trip trip, List<DailyWeatherDTO> weatherList, ModifyPlanDTO modifyPlanDTO) {
        Assert.notNull(trip, "Trip must not be null");
        Assert.notNull(modifyPlanDTO, "ModifyPlanDTO must not be null");

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are an expert travel planner. Regenerate the itinerary using the existing trip overview below.
                The user has added NEW STRICT PREFERENCES that MUST be enforced. Treat them as hard constraints.

                Trip Overview:
                """);

        appendTripOverview(prompt, trip);

        prompt.append("\nExisting Notes (lower priority):\n");
        prompt.append(String.format("- Notes: %s%n", defaultString(trip.getPreferences(), "No additional preferences")));

        appendWeather(prompt, weatherList);

        prompt.append("\nNEW STRICT PREFERENCES (highest priority, MUST be enforced):\n");
        String strict = modifyPlanDTO.getSecondPreference();
        prompt.append(String.format("- %s%n", defaultString(strict, "No new strict preferences provided")));

        prompt.append("\nIf any previous plan conflicts with these strict preferences, you MUST adjust it accordingly.\n");
        appendInstructions(prompt);
        return prompt.toString();
    }

    private void appendTripOverview(StringBuilder prompt, Trip preference) {
        String fromCity = defaultString(preference.getFromCity(), "Unknown city");
        String fromCountry = defaultString(preference.getFromCountry(), "Unknown country");
        String toCity = defaultString(preference.getToCity(), "Unknown city");
        String toCountry = defaultString(preference.getToCountry(), "Unknown country");
        LocalDate startDate = preference.getStartDate();
        LocalDate endDate = preference.getEndDate();
        Long duration = calculateDuration(startDate, endDate);

        prompt.append(String.format("- Departure: %s, %s%n", fromCity, fromCountry));
        prompt.append(String.format("- Destination: %s, %s%n", toCity, toCountry));
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
        prompt.append('\n');
        prompt.append(String.format(
        """
        Hard constraints:
            Routing:
                - The traveler STARTS in "%s, %s" (Departure) and ENDS in "%s, %s" (Destination). Do not swap them.
                - On day 1, depart from "%s" to "%s".
                - On Day 1, the FIRST item in "activities" MUST be a transportation event: { "type":"transportation", "from":"%s", "to":"%s" }.
                - On the final day, return from "%s" to "%s".
                - On the FINAL day, the LAST item in "activities" MUST be a transportation event: { "type":"transportation", "from":"%s", "to":"%s" }.
            Hotels:
                - Day 1 MUST include one hotel check-in in "%s".
                - If the trip lasts over 3 days, aim to add 1–2 additional hotel check-ins beyond Day 1 in "%s".
            Attractions per day:
                - Multiple "attraction" items are allowed per day; NEVER more than 5.
                - Prefer 3–4 attractions per full sightseeing day (2–3 acceptable on light or travel-heavy days).
                - Search for real restaurants, cafe, which have authentic local cuisine.
            Transportation on non-first/last days:
                - You MAY add transportation only within "%s"; both "from" and "to" MUST be within "%s".
                - Prefer public transit: "bus", "subway"/"metro", "train". Avoid flights on these days.
        """, fromCity, fromCountry, toCity, toCountry, fromCity, toCity, fromCity, toCity, toCity, fromCity, toCity, fromCity,
                        toCity, toCity, toCity, toCity)
        );
    }

    private void appendPreferences(StringBuilder prompt, Trip preference) {
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

                    You must return **ONLY** a JSON object with two keys: "daily_summaries" and "activities".
                    During the travel dates, "daily_summaries" MUST contain exactly one entry per calendar day. For example, If travel dates are 2025-11-01 to 2025-11-12, then "daily_summaries" must have 12 entries dated.

                    "daily_summaries" is a list of objects, each with (in order):
                        - "date": travel date in yyyy-MM-dd format
                        - "summary": a short natural language description of the day
                        - "image_description": a short and adaptable Unsplash search phrase in ENGLISH (3–7 words)

                    "activities" is a list of scheduled activities. Each activity has (in order):
                        - "date": yyyy-MM-dd
                        - "type": one of "transportation", "hotel", or "attraction"
                        - "time": HH:mm format (e.g., "14:00")
                        - "title": short title to show on a timeline
                        - "status": always set to "pending"
                        - "reservation_required": true if booking is needed, else false
                        - "image_description": a short and adaptable Unsplash search phrase in ENGLISH (3–7 words)

                    For "transportation" type, include (in order):
                        - "from": starting location
                        - "to": destination
                        - "provider": airline/bus company/etc.
                        - "ticket_type": e.g., "economy"
                        - "price": number
                        - "currency": e.g., "JPY" or "AUD"
                        - "image_description": a short and adaptable Unsplash search phrase in ENGLISH (3–7 words)

                    For "hotel" type, include (in order):
                        - "hotel_name": hotel name
                        - "room_type": e.g., "Double room"
                        - "people": number of guests
                        - "nights": how many nights
                        - "price": number
                        - "currency": e.g., "JPY"
                        - "image_description": a short and adaptable Unsplash search phrase in ENGLISH (3–7 words)

                    For "attraction" type (includes restaurants, parks, temples) (in order):
                        - "location": e.g., "Shinjuku, Tokyo"
                        - "ticket_price": number (meal or entry cost)
                        - "people": number of attendees
                        - "currency": e.g., "JPY"
                        - "image_description": a short and adaptable Unsplash search phrase in ENGLISH (3–7 words)

                    Scheduling rules:
                        - All fields MUST have a value, cannot be null! Do not invent impossible data.
                        - For EVERY calendar date within the travel window, there MUST be AT LEAST ONE item in "activities" whose "date" equals that day. Empty days are NOT allowed.

                    Do NOT return markdown, explanation, or any wrapper text. Just return pure JSON object, well-formatted!
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