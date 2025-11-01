package com.demo.api.support;

import com.demo.api.dto.TripDetailDTO;
import com.demo.api.model.Trip;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripDailySummary;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripTransportation;
import com.demo.api.model.TripWeather;

import java.time.LocalDate;

/**
 * Simple factory helpers for assembling domain objects in tests without piling up builder noise.
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Trip trip(long id) {
        return Trip.builder()
                .id(id)
                .userId(101L)
                .fromCountry("AU")
                .fromCity("Sydney")
                .toCountry("JP")
                .toCity("Tokyo")
                .currency("AUD")
                .budget(2500)
                .people(2)
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 3, 7))
                .build();
    }

    public static TripDailySummary summary(long id, long tripId, LocalDate date) {
        TripDailySummary summary = new TripDailySummary();
        summary.setId(id);
        summary.setTripId(tripId);
        summary.setDate(date);
        summary.setSummary("Day " + id + " summary");
        summary.setImageUrl("https://cdn.example.com/image-" + id + ".jpg");
        return summary;
    }

    public static TripTransportation transport(long tripId, LocalDate date) {
        return TripTransportation.builder()
                .tripId(tripId)
                .date(date)
                .time("09:00")
                .title("Flight to Osaka")
                .status("pending")
                .from("SYD")
                .to("KIX")
                .build();
    }

    public static TripHotel hotel(long tripId, LocalDate date) {
        return TripHotel.builder()
                .tripId(tripId)
                .date(date)
                .time("15:00")
                .title("Check in")
                .status("pending")
                .hotelName("Ryokan Sakura")
                .roomType("Double")
                .nights(1)
                .people(2)
                .currency("JPY")
                .price(180)
                .build();
    }

    public static TripAttraction attraction(long tripId, LocalDate date) {
        return TripAttraction.builder()
                .tripId(tripId)
                .date(date)
                .time("12:00")
                .title("Shrine visit")
                .status("pending")
                .location("Kyoto")
                .ticketPrice(40)
                .people(2)
                .currency("JPY")
                .build();
    }

    public static TripWeather weather(long tripId, LocalDate date) {
        return TripWeather.builder()
                .tripId(tripId)
                .date(date)
                .minTemp(12.0)
                .maxTemp(22.0)
                .weatherCondition("Sunny")
                .build();
    }

    public static TripDetailDTO tripDetailFrom(Trip trip, String imageUrl) {
        return TripDetailDTO.builder()
                .tripId(trip.getId())
                .fromCountry(trip.getFromCountry())
                .fromCity(trip.getFromCity())
                .toCountry(trip.getToCountry())
                .toCity(trip.getToCity())
                .budget(trip.getBudget())
                .people(trip.getPeople())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .imgUrl(imageUrl)
                .build();
    }
}

