package com.demo.api;

import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.model.Trip;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripBookingQuote;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripTransportation;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripBookingQuoteRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.demo.api.service.impl.BookingServiceImpl;
import com.demo.api.support.IntegrationTestSupport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Transactional
@Tag("integration")
class BookingServiceIT extends IntegrationTestSupport {

    @Autowired
    private BookingServiceImpl bookingService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripHotelRepository tripHotelRepository;

    @Autowired
    private TripTransportationRepository tripTransportationRepository;

    @Autowired
    private TripAttractionRepository tripAttractionRepository;

    @Autowired
    private TripBookingQuoteRepository tripBookingQuoteRepository;

    private Trip preference;

    @BeforeEach
    void setUpPreference() {
        tripBookingQuoteRepository.deleteAll();
        tripAttractionRepository.deleteAll();
        tripTransportationRepository.deleteAll();
        tripHotelRepository.deleteAll();
        tripRepository.deleteAll();

        preference = tripRepository.save(Trip.builder()
                .currency("AUD")
                .people(2)
                .toCity("Sydney")
                .build());

        clearBookingServerRequests();
    }

    @DisplayName("quoteSingleItem persists returned booking quote")
    @Test
    void quoteSingleItemPersistsQuote() {
        TripHotel hotel = tripHotelRepository.save(TripHotel.builder()
                .tripId(preference.getId())
                .date(LocalDate.now().plusDays(7))
                .hotelName("Harbour Hotel")
                .roomType("Deluxe")
                .nights(2)
                .currency("AUD")
                .reservationRequired(true)
                .status("pending")
                .build());

        enqueueQuoteResponse(hotel.getId());

        TripBookingQuote result = bookingService.quoteSingleItem(preference.getId(), "hotel", hotel.getId());

        Assertions.assertThat(result.getVoucherCode()).isEqualTo("VCH-1234-ABCD");
        Assertions.assertThat(result.getInvoiceId()).isEqualTo("INV_9876");
        Assertions.assertThat(result.getTripId()).isEqualTo(preference.getId());
        Assertions.assertThat(result.getEntityId()).isEqualTo(hotel.getId());
        Assertions.assertThat(result.getStatus()).isEqualTo("confirm");
        Assertions.assertThat(result.getTotalAmount()).isEqualTo(150);
        Assertions.assertThat(result.getItemReference()).isEqualTo("hotel_" + hotel.getId());

        RecordedRequest recordedRequest = awaitRequest();
        Assertions.assertThat(recordedRequest.getPath()).isEqualTo("/quote");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getBody().readUtf8())
                .contains("\"product_type\":\"hotel\"")
                .contains("\"trip_id\":" + preference.getId())
                .contains("\"entity_id\":" + hotel.getId());
    }

    @DisplayName("quoteItinerary stores each item returned by Booking API")
    @Test
    void quoteItineraryStoresQuotesForEachItem() {
        TripTransportation transport = tripTransportationRepository.save(TripTransportation.builder()
                .tripId(preference.getId())
                .date(LocalDate.now().plusDays(3))
                .from("SYD")
                .to("MEL")
                .provider("Qantas Airways")
                .reservationRequired(true)
                .status("pending")
                .build());

        TripAttraction attraction = tripAttractionRepository.save(TripAttraction.builder()
                .tripId(preference.getId())
                .date(LocalDate.now().plusDays(4))
                .title("Opera House Tour")
                .location("Sydney")
                .reservationRequired(true)
                .status("pending")
                .build());

        enqueueItineraryResponse(transport.getId(), attraction.getId());

        ItineraryQuoteResp response = bookingService.quoteItinerary(preference.getId());

        Assertions.assertThat(response.voucherCode()).isEqualTo("VCH-ITI-456");

        List<TripBookingQuote> quotes = tripBookingQuoteRepository.findByTripId(preference.getId());
        Assertions.assertThat(quotes).hasSize(2);
        Assertions.assertThat(quotes).allMatch(quote -> "confirm".equals(quote.getStatus()));
        Assertions.assertThat(quotes)
                .extracting(TripBookingQuote::getItemReference)
                .containsExactlyInAnyOrder(
                        "transport_" + transport.getId(),
                        "attraction_" + attraction.getId()
                );

        RecordedRequest recordedRequest = awaitRequest();
        Assertions.assertThat(recordedRequest.getPath()).isEqualTo("/itinerary/quote");
    }

    private void enqueueQuoteResponse(long hotelId) {
        String quoteResponseJson = """
                {
                  "voucher_code": "VCH-1234-ABCD",
                  "invoice_id": "INV_9876",
                  "items": [
                    {
                      "sku": "HTL_%d",
                      "unit_price": 150,
                      "quantity": 1,
                      "fees": 0,
                      "total": 150,
                      "currency": "AUD",
                      "meta": {
                        "hotel_name": "Harbour Hotel"
                      },
                      "cancellation_policy": "Free cancellation up to 24h"
                    }
                  ]
                }
                """.formatted(hotelId);
        bookingServer().enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(quoteResponseJson));
    }

    private void enqueueItineraryResponse(long transportId, long attractionId) {
        String itineraryResponseJson = """
                {
                  "voucher_code": "VCH-ITI-456",
                  "invoice_id": "INV_5555",
                  "currency": "AUD",
                  "items": [
                    {
                      "reference": "transport_%d",
                      "product_type": "transportation",
                      "party_size": 2,
                      "total": 300,
                      "fees": 0,
                      "quote_items": [
                        {
                          "sku": "SKU_FLIGHT",
                          "unit_price": 150,
                          "quantity": 2,
                          "fees": 0,
                          "total": 300,
                          "currency": "AUD",
                          "meta": {
                            "provider": "Qantas Airways"
                          }
                        }
                      ]
                    },
                    {
                      "reference": "attraction_%d",
                      "product_type": "attraction",
                      "party_size": 2,
                      "total": 120,
                      "fees": 0,
                      "quote_items": [
                        {
                          "sku": "SKU_TOUR",
                          "unit_price": 60,
                          "quantity": 2,
                          "fees": 0,
                          "total": 120,
                          "currency": "AUD",
                          "meta": {
                            "location": "Sydney"
                          }
                        }
                      ]
                    }
                  ],
                  "bundle_total": 420,
                  "bundle_fees": 0
                }
                """.formatted(transportId, attractionId);
        bookingServer().enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(itineraryResponseJson));
    }

    private RecordedRequest awaitRequest() {
        try {
            RecordedRequest request = bookingServer().takeRequest(2, TimeUnit.SECONDS);
            if (request == null) {
                throw new AssertionError("Expected Booking API request but none received");
            }
            return request;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting booking request", e);
        }
    }

    private void clearBookingServerRequests() {
        try {
            RecordedRequest leftover;
            while ((leftover = bookingServer().takeRequest(10, TimeUnit.MILLISECONDS)) != null) {
                // discard any unconsumed requests from previous tests
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while clearing booking server requests", e);
        }
    }
}
