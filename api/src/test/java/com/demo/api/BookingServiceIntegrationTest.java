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
                Map.of("hotel_name", "Harbour Hotel"),
                "Free cancellation up to 24h"
        );
        QuoteResp quoteResp = new QuoteResp(
                "VCH-1234-ABCD",
                "INV_9876",
                List.of(quoteItem)
        );
        when(bookingApiClient.postQuote(any())).thenReturn(quoteResp);

        TripBookingQuote result = bookingService.quoteSingleItem(preference.getId(), "hotel", hotel.getId());

        assertThat(result.getVoucherCode()).isEqualTo("VCH-1234-ABCD");
        assertThat(result.getInvoiceId()).isEqualTo("INV_9876");
        assertThat(result.getTripId()).isEqualTo(preference.getId());
        assertThat(result.getEntityId()).isEqualTo(hotel.getId());
        assertThat(result.getStatus()).isEqualTo("confirm");
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
                "VCH-ITI-456",
                "INV_5555",
                "AUD",
                List.of(transportItem, attractionItem),
                BigDecimal.valueOf(420),
                BigDecimal.ZERO
        );
        when(bookingApiClient.postItineraryQuote(any())).thenReturn(itineraryQuoteResp);

        ItineraryQuoteResp response = bookingService.quoteItinerary(preference.getId());

        assertThat(response.voucherCode()).isEqualTo("VCH-ITI-456");

        List<TripBookingQuote> quotes = tripBookingQuoteRepository.findByTripId(preference.getId());
        assertThat(quotes).hasSize(2);
        assertThat(quotes).allMatch(quote -> "confirm".equals(quote.getStatus()));
        assertThat(quotes)
                .extracting(TripBookingQuote::getItemReference)
                .containsExactlyInAnyOrder(
                        "transport_" + transport.getId(),
                        "attraction_" + attraction.getId()
                );

        verify(bookingApiClient, times(1)).postItineraryQuote(any());
    }

}
