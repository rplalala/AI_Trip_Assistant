package com.demo.api;

import com.demo.api.client.BookingApiClient;
import com.demo.api.dto.booking.*;
import com.demo.api.model.*;
import com.demo.api.repository.*;
import com.demo.api.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class BookingServiceIntegrationTest {

    @Autowired
    private BookingServiceImpl bookingService;

    @Autowired
    private TripPreferenceRepository tripPreferenceRepository;

    @Autowired
    private TripHotelRepository tripHotelRepository;

    @Autowired
    private TripTransportationRepository tripTransportationRepository;

    @Autowired
    private TripAttractionRepository tripAttractionRepository;

    @Autowired
    private TripBookingQuoteRepository tripBookingQuoteRepository;

    @MockBean
    private BookingApiClient bookingApiClient;

    private TripPreference preference;

    @BeforeEach
    void setUpPreference() {
        tripBookingQuoteRepository.deleteAll();
        tripAttractionRepository.deleteAll();
        tripTransportationRepository.deleteAll();
        tripHotelRepository.deleteAll();
        tripPreferenceRepository.deleteAll();

        preference = tripPreferenceRepository.save(TripPreference.builder()
                .currency("AUD")
                .people(2)
                .toCity("Sydney")
                .build());
    }

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

        QuoteItem quoteItem = new QuoteItem(
                "HTL_" + hotel.getId(),
                BigDecimal.valueOf(150),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(150),
                "AUD",
                999,
                Map.of("hotel_name", "Harbour Hotel"),
                "Free cancellation up to 24h"
        );
        QuoteResp quoteResp = new QuoteResp(
                "qt_hotel_123",
                OffsetDateTime.now().plusHours(1),
                List.of(quoteItem)
        );
        when(bookingApiClient.postQuote(any())).thenReturn(quoteResp);

        TripBookingQuote result = bookingService.quoteSingleItem(preference.getId(), "hotel", hotel.getId());

        assertThat(result.getQuoteToken()).isEqualTo("qt_hotel_123");
        assertThat(result.getTripId()).isEqualTo(preference.getId());
        assertThat(result.getEntityId()).isEqualTo(hotel.getId());
        assertThat(result.getStatus()).isEqualTo("quoted");
        assertThat(result.getTotalAmount()).isEqualTo(150);
        assertThat(result.getItemReference()).isEqualTo("hotel_" + hotel.getId());

        verify(bookingApiClient, times(1)).postQuote(any());
    }

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

        QuoteItem transportLineItem = new QuoteItem(
                "SKU_FLIGHT",
                BigDecimal.valueOf(150),
                2,
                BigDecimal.ZERO,
                BigDecimal.valueOf(300),
                "AUD",
                50,
                Map.of("provider", "Qantas Airways"),
                null
        );
        ItineraryQuoteItem transportItem = new ItineraryQuoteItem(
                "transport_" + transport.getId(),
                "transportation",
                2,
                BigDecimal.valueOf(300),
                BigDecimal.ZERO,
                List.of(transportLineItem)
        );
        QuoteItem attractionLineItem = new QuoteItem(
                "SKU_TOUR",
                BigDecimal.valueOf(60),
                2,
                BigDecimal.ZERO,
                BigDecimal.valueOf(120),
                "AUD",
                75,
                Map.of("location", "Sydney"),
                null
        );
        ItineraryQuoteItem attractionItem = new ItineraryQuoteItem(
                "attraction_" + attraction.getId(),
                "attraction",
                2,
                BigDecimal.valueOf(120),
                BigDecimal.ZERO,
                List.of(attractionLineItem)
        );

        ItineraryQuoteResp itineraryQuoteResp = new ItineraryQuoteResp(
                "iti_qt_456",
                OffsetDateTime.now().plusHours(2),
                "AUD",
                List.of(transportItem, attractionItem),
                BigDecimal.valueOf(420),
                BigDecimal.ZERO
        );
        when(bookingApiClient.postItineraryQuote(any())).thenReturn(itineraryQuoteResp);

        ItineraryQuoteResp response = bookingService.quoteItinerary(preference.getId());

        assertThat(response.quoteToken()).isEqualTo("iti_qt_456");

        List<TripBookingQuote> quotes = tripBookingQuoteRepository.findByTripId(preference.getId());
        assertThat(quotes).hasSize(2);
        assertThat(quotes).allMatch(quote -> "quoted".equals(quote.getStatus()));
        assertThat(quotes)
                .extracting(TripBookingQuote::getItemReference)
                .containsExactlyInAnyOrder(
                        "transport_" + transport.getId(),
                        "attraction_" + attraction.getId()
                );

        verify(bookingApiClient, times(1)).postItineraryQuote(any());
    }

    @Test
    void confirmBookingUpdatesStatus() {
        // Seed quotes via itinerary call
        TripTransportation transport = tripTransportationRepository.save(TripTransportation.builder()
                .tripId(preference.getId())
                .date(LocalDate.now().plusDays(3))
                .from("SYD")
                .to("MEL")
                .provider("Qantas Airways")
                .reservationRequired(true)
                .status("pending")
                .build());

        QuoteItem transportLineItem = new QuoteItem(
                "SKU_FLIGHT",
                BigDecimal.valueOf(150),
                2,
                BigDecimal.ZERO,
                BigDecimal.valueOf(300),
                "AUD",
                50,
                Map.of("provider", "Qantas Airways"),
                null
        );
        ItineraryQuoteItem transportItem = new ItineraryQuoteItem(
                "transport_" + transport.getId(),
                "transportation",
                2,
                BigDecimal.valueOf(300),
                BigDecimal.ZERO,
                List.of(transportLineItem)
        );
        ItineraryQuoteResp itineraryQuoteResp = new ItineraryQuoteResp(
                "iti_qt_789",
                OffsetDateTime.now().plusHours(2),
                "AUD",
                List.of(transportItem),
                BigDecimal.valueOf(300),
                BigDecimal.ZERO
        );
        when(bookingApiClient.postItineraryQuote(any())).thenReturn(itineraryQuoteResp);
        bookingService.quoteItinerary(preference.getId());

        ConfirmResp confirmResponse = new ConfirmResp(
                "CONFIRMED",
                "voucher123",
                "inv456"
        );
        when(bookingApiClient.postConfirm(any(), any())).thenReturn(confirmResponse);

        ConfirmResp confirmResp = bookingService.confirmBooking("iti_qt_789", List.of("transport_" + transport.getId()));

        assertThat(confirmResp.status()).isEqualTo("CONFIRMED");

        TripBookingQuote quote = tripBookingQuoteRepository.findByQuoteTokenAndItemReference("iti_qt_789", "transport_" + transport.getId())
                .orElseThrow();
        assertThat(quote.getStatus()).isEqualTo("confirmed");

        TripTransportation updatedTransport = tripTransportationRepository.findById(transport.getId()).orElseThrow();
        assertThat(updatedTransport.getStatus()).isEqualTo("confirmed");

        verify(bookingApiClient, times(1)).postConfirm(any(), any());
    }
}
