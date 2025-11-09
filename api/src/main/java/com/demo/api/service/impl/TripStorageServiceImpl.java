package com.demo.api.service.impl;

import com.demo.api.dto.ItineraryDTO;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripDailySummary;
import com.demo.api.model.TripHotel;
import com.demo.api.model.Trip;
import com.demo.api.model.TripTransportation;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripDailySummaryRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.demo.api.service.TripStorageService;
import com.demo.api.utils.UnsplashImgUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists the structured itinerary returned by the AI planner into the trip_* tables.
 */
@Service
@RequiredArgsConstructor
public class TripStorageServiceImpl implements TripStorageService {

    private static final Logger log = LoggerFactory.getLogger(TripStorageServiceImpl.class);
    private static final String DEFAULT_STATUS = "pending";

    private final TripTransportationRepository tripTransportationRepository;
    private final TripHotelRepository tripHotelRepository;
    private final TripAttractionRepository tripAttractionRepository;
    private final TripDailySummaryRepository tripDailySummaryRepository;
    private final UnsplashImgUtils unsplashImgUtils;

    @Override
    @Transactional
    public void storeTripPlan(Trip preference, ItineraryDTO itinerary) {
        if (preference == null || preference.getId() == null) {
            throw new IllegalArgumentException("Trip preference with persistent tripId is required");
        }
        if (itinerary == null) {
            log.warn("Empty itinerary DTO for trip {}", preference.getId());
            return;
        }

        try {
            // Daily summaries
            tripDailySummaryRepository.deleteAll(tripDailySummaryRepository.findByTripId(preference.getId()));
            List<TripDailySummary> summariesToSave = readDailySummaries(itinerary.getDailySummaries(), preference);
            tripDailySummaryRepository.saveAll(summariesToSave);

            // Clear previously generated activities for idempotency.
            tripTransportationRepository.deleteAll(tripTransportationRepository.findByTripId(preference.getId()));
            tripHotelRepository.deleteAll(tripHotelRepository.findByTripId(preference.getId()));
            tripAttractionRepository.deleteAll(tripAttractionRepository.findByTripId(preference.getId()));

            List<TripTransportation> transportation = new ArrayList<>();
            List<TripHotel> hotels = new ArrayList<>();
            List<TripAttraction> attractions = new ArrayList<>();

            if (itinerary.getActivities() != null) {
                for (ItineraryDTO.ActivityDTO activity : itinerary.getActivities()) {
                    String type = activity.getType() == null ? "" : activity.getType().toLowerCase();
                    switch (type) {
                        case "transportation" -> transportation.add(mapTransportation((ItineraryDTO.TransportationDTO) activity, preference));
                        case "hotel" -> hotels.add(mapHotel((ItineraryDTO.HotelDTO) activity, preference));
                        case "attraction" -> attractions.add(mapAttraction((ItineraryDTO.AttractionDTO) activity, preference));
                        default -> log.debug("Skipping activity with unsupported type '{}' for trip {}", type, preference.getId());
                    }
                }
            }

            tripTransportationRepository.saveAll(transportation);
            tripHotelRepository.saveAll(hotels);
            tripAttractionRepository.saveAll(attractions);

            log.info("Stored trip plan for trip {} ({} transportation / {} hotels / {} attractions)",
                    preference.getId(), transportation.size(), hotels.size(), attractions.size());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist trip plan DTO", ex);
        }
    }

    // ----- mapping helpers ------

    private List<TripDailySummary> readDailySummaries(List<ItineraryDTO.DailySummaryDTO> summaries, Trip preference) {
        List<TripDailySummary> result = new ArrayList<>();
        if (summaries == null) return result;
        for (ItineraryDTO.DailySummaryDTO dto : summaries) {
            LocalDate date = dto.getDate();
            if (date == null) continue;

            TripDailySummary summary = new TripDailySummary();
            summary.setTripId(preference.getId());
            summary.setDate(date);
            summary.setSummary(dto.getSummary());
            summary.setImageDescription(dto.getImageDescription());

            String imageUrl = dto.getImageUrl();
            if (!StringUtils.hasText(imageUrl)) {
                try {
                    var imgUrls = unsplashImgUtils.getImgUrls(dto.getImageDescription(), 1, 500, 500);
                    if (imgUrls != null && !imgUrls.isEmpty()) {
                        imageUrl = imgUrls.getFirst();
                    } else {
                        imageUrl = "";
                    }
                } catch (Exception e) {
                    log.warn("Failed to load image urls for trip {}", preference.getId(), e);
                    imageUrl = "";
                }
            }
            summary.setImageUrl(imageUrl);

            result.add(summary);
        }
        return result;
    }

    private TripTransportation mapTransportation(ItineraryDTO.TransportationDTO dto, Trip preference) {
        TripTransportation transport = new TripTransportation();
        populateCommonActivityFields(transport, dto, preference);
        transport.setFrom(dto.getFrom());
        transport.setTo(dto.getTo());
        transport.setProvider(dto.getProvider());
        transport.setTicketType(dto.getTicketType());
        transport.setPrice(dto.getPrice());
        transport.setCurrency(StringUtils.hasText(dto.getCurrency()) ? dto.getCurrency() : defaultCurrency(preference));
        return transport;
    }

    private TripHotel mapHotel(ItineraryDTO.HotelDTO dto, Trip preference) {
        TripHotel hotel = new TripHotel();
        populateCommonActivityFields(hotel, dto, preference);
        hotel.setHotelName(dto.getHotelName());
        hotel.setRoomType(dto.getRoomType());
        hotel.setPeople(dto.getPeople());
        hotel.setNights(dto.getNights());
        hotel.setPrice(dto.getPrice());
        hotel.setCurrency(StringUtils.hasText(dto.getCurrency()) ? dto.getCurrency() : defaultCurrency(preference));
        return hotel;
    }

    private TripAttraction mapAttraction(ItineraryDTO.AttractionDTO dto, Trip preference) {
        TripAttraction attraction = new TripAttraction();
        populateCommonActivityFields(attraction, dto, preference);
        attraction.setLocation(dto.getLocation());
        attraction.setTicketPrice(dto.getTicketPrice());
        attraction.setPeople(dto.getPeople());
        attraction.setCurrency(StringUtils.hasText(dto.getCurrency()) ? dto.getCurrency() : defaultCurrency(preference));
        return attraction;
    }

    private void populateCommonActivityFields(Object target, ItineraryDTO.ActivityDTO dto, Trip preference) {
        LocalDate date = dto.getDate();
        String time = dto.getTime();
        String title = dto.getTitle();
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : DEFAULT_STATUS;
        Boolean reservationRequired = dto.getReservationRequired();
        String imageDescription = dto.getImageDescription();
        String imageUrl = StringUtils.hasText(dto.getImageUrl()) ? dto.getImageUrl() : "";

        if (target instanceof TripTransportation transport) {
            transport.setTripId(preference.getId());
            transport.setDate(date);
            transport.setTime(time);
            transport.setTitle(title);
            transport.setStatus(status);
            transport.setReservationRequired(reservationRequired);
            transport.setImageUrl(imageUrl);
            transport.setImageDescription(imageDescription);
        } else if (target instanceof TripHotel hotel) {
            hotel.setTripId(preference.getId());
            hotel.setDate(date);
            hotel.setTime(time);
            hotel.setTitle(title);
            hotel.setStatus(status);
            hotel.setReservationRequired(reservationRequired);
            hotel.setImageUrl(imageUrl);
            hotel.setImageDescription(imageDescription);
        } else if (target instanceof TripAttraction attraction) {
            attraction.setTripId(preference.getId());
            attraction.setDate(date);
            attraction.setTime(time);
            attraction.setTitle(title);
            attraction.setStatus(status);
            attraction.setReservationRequired(reservationRequired);
            attraction.setImageUrl(imageUrl);
            attraction.setImageDescription(imageDescription);
        }
    }

    private String defaultCurrency(Trip preference) {
        return StringUtils.hasText(preference.getCurrency()) ? preference.getCurrency() : "AUD";
    }
}
