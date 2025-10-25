package com.demo.api.service.impl;

import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripDailySummary;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripPreference;
import com.demo.api.model.TripTransportation;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripDailySummaryRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.demo.api.service.TripStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Persists the structured itinerary returned by the AI planner into the trip_* tables.
 */
@Service
@RequiredArgsConstructor
public class TripStorageServiceImpl implements TripStorageService {

    private static final Logger log = LoggerFactory.getLogger(TripStorageServiceImpl.class);
    private static final String DEFAULT_STATUS = "pending";

    private final ObjectMapper objectMapper;
    private final TripTransportationRepository tripTransportationRepository;
    private final TripHotelRepository tripHotelRepository;
    private final TripAttractionRepository tripAttractionRepository;
    private final TripDailySummaryRepository tripDailySummaryRepository;

    @Override
    @Transactional
    public void storeTripPlan(TripPreference preference, String tripPlanJson) {
        if (preference == null || preference.getTripId() == null) {
            throw new IllegalArgumentException("Trip preference with persistent tripId is required");
        }
        if (!StringUtils.hasText(tripPlanJson)) {
            log.warn("Empty trip plan JSON for trip {}", preference.getTripId());
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(tripPlanJson);

            tripDailySummaryRepository.deleteAll(tripDailySummaryRepository.findByTripId(preference.getTripId()));
            List<TripDailySummary> summariesToSave = readDailySummaries(root.path("daily_summaries"), preference);
            tripDailySummaryRepository.saveAll(summariesToSave);

            // Clear previously generated activities for idempotency.
            tripTransportationRepository.deleteAll(tripTransportationRepository.findByTripId(preference.getTripId()));
            tripHotelRepository.deleteAll(tripHotelRepository.findByTripId(preference.getTripId()));
            tripAttractionRepository.deleteAll(tripAttractionRepository.findByTripId(preference.getTripId()));

            ArrayNode activitiesNode = root.has("activities") && root.get("activities").isArray()
                    ? (ArrayNode) root.get("activities")
                    : objectMapper.createArrayNode();

            List<TripTransportation> transportation = new ArrayList<>();
            List<TripHotel> hotels = new ArrayList<>();
            List<TripAttraction> attractions = new ArrayList<>();

            for (JsonNode activityNode : activitiesNode) {
                String type = optionalText(activityNode, "type").orElse("").toLowerCase(Locale.ENGLISH);
                switch (type) {
                    case "transportation" -> transportation.add(mapTransportation(activityNode, preference));
                    case "hotel" -> hotels.add(mapHotel(activityNode, preference));
                    case "attraction" -> attractions.add(mapAttraction(activityNode, preference));
                    default -> log.debug("Skipping activity with unsupported type '{}' for trip {}", type, preference.getTripId());
                }
            }

            tripTransportationRepository.saveAll(transportation);
            tripHotelRepository.saveAll(hotels);
            tripAttractionRepository.saveAll(attractions);

            log.info("Stored trip plan for trip {} ({} transportation / {} hotels / {} attractions)",
                    preference.getTripId(), transportation.size(), hotels.size(), attractions.size());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist trip plan JSON", ex);
        }
    }

    private List<TripDailySummary> readDailySummaries(JsonNode node, TripPreference preference) {
        List<TripDailySummary> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode summaryNode : node) {
                LocalDate date = parseDate(optionalText(summaryNode, "date"));
                if (date == null) {
                    continue;
                }
                TripDailySummary summary = new TripDailySummary();
                summary.setTripId(preference.getTripId());
                summary.setDate(date);
                summary.setSummary(optionalText(summaryNode, "summary").orElse(null));
                result.add(summary);
            }
        }
        return result;
    }

    private TripTransportation mapTransportation(JsonNode node, TripPreference preference) {
        TripTransportation transport = new TripTransportation();
        populateCommonActivityFields(transport, node, preference);
        transport.setFrom(optionalText(node, "from").orElse(null));
        transport.setTo(optionalText(node, "to").orElse(null));
        transport.setProvider(optionalText(node, "provider").orElse(null));
        transport.setTicketType(optionalText(node, "ticket_type").orElse(null));
        transport.setPrice(asInteger(node.get("price")));
        transport.setCurrency(optionalText(node, "currency").orElse(defaultCurrency(preference)));
        return transport;
    }

    private TripHotel mapHotel(JsonNode node, TripPreference preference) {
        TripHotel hotel = new TripHotel();
        populateCommonActivityFields(hotel, node, preference);
        hotel.setHotelName(optionalText(node, "hotel_name").orElse(null));
        hotel.setRoomType(optionalText(node, "room_type").orElse(null));
        hotel.setPeople(asInteger(node.get("people")));
        hotel.setNights(asInteger(node.get("nights")));
        hotel.setPrice(asInteger(node.get("price")));
        hotel.setCurrency(optionalText(node, "currency").orElse(defaultCurrency(preference)));
        return hotel;
    }

    private TripAttraction mapAttraction(JsonNode node, TripPreference preference) {
        TripAttraction attraction = new TripAttraction();
        populateCommonActivityFields(attraction, node, preference);
        attraction.setLocation(optionalText(node, "location").orElse(null));
        attraction.setTicketPrice(asInteger(node.get("ticket_price")));
        attraction.setPeople(asInteger(node.get("people")));
        attraction.setCurrency(optionalText(node, "currency").orElse(defaultCurrency(preference)));
        return attraction;
    }

    private void populateCommonActivityFields(Object target, JsonNode node, TripPreference preference) {
        LocalDate date = parseDate(optionalText(node, "date"));
        String time = optionalText(node, "time").orElse(null);
        String title = optionalText(node, "title").orElse(null);
        String status = optionalText(node, "status").orElse(DEFAULT_STATUS);
        Boolean reservationRequired = node.hasNonNull("reservation_required") ? node.get("reservation_required").asBoolean() : null;
        String imageUrl = optionalText(node, "image_url").orElse(null);

        if (target instanceof TripTransportation transport) {
            transport.setTripId(preference.getTripId());
            transport.setDate(date);
            transport.setTime(time);
            transport.setTitle(title);
            transport.setStatus(status);
            transport.setReservationRequired(reservationRequired);
            transport.setImageUrl(imageUrl);
        } else if (target instanceof TripHotel hotel) {
            hotel.setTripId(preference.getTripId());
            hotel.setDate(date);
            hotel.setTime(time);
            hotel.setTitle(title);
            hotel.setStatus(status);
            hotel.setReservationRequired(reservationRequired);
            hotel.setImageUrl(imageUrl);
        } else if (target instanceof TripAttraction attraction) {
            attraction.setTripId(preference.getTripId());
            attraction.setDate(date);
            attraction.setTime(time);
            attraction.setTitle(title);
            attraction.setStatus(status);
            attraction.setReservationRequired(reservationRequired);
            attraction.setImageUrl(imageUrl);
        }
    }

    private Optional<String> optionalText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return Optional.empty();
        }
        String value = node.get(field).asText();
        return StringUtils.hasText(value) ? Optional.of(value) : Optional.empty();
    }

    private LocalDate parseDate(Optional<String> value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.get());
        } catch (Exception ex) {
            log.debug("Unable to parse date value '{}'", value.get(), ex);
            return null;
        }
    }

    private Integer asInteger(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.intValue();
        }
        if (node.isNumber()) {
            BigDecimal decimal = node.decimalValue();
            return decimal.setScale(0, RoundingMode.HALF_UP).intValue();
        }
        try {
            return new BigDecimal(node.asText()).setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (Exception ex) {
            log.debug("Unable to convert value '{}' to integer", node, ex);
            return null;
        }
    }

    private String defaultCurrency(TripPreference preference) {
        return StringUtils.hasText(preference.getCurrency()) ? preference.getCurrency() : "AUD";
    }
}
