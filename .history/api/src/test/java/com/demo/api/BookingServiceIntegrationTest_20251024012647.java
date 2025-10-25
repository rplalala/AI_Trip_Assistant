package com.demo.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.demo.api.client.BookingApiClient;
import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteItem;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.model.TripAttraction;
import com.demo.api.model.TripBookingQuote;
import com.demo.api.model.TripHotel;
import com.demo.api.model.TripPreference;
import com.demo.api.model.TripTransportation;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripBookingQuoteRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripPreferenceRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.demo.api.service.booking.BookingService;

@SpringBootTest
@Transactional
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

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
                .tripId(preference.getTripId())
                .date(LocalDate.now().plusDays(7))
                .hotelName("Harbour Hotel")
                .roomType("Deluxe")
                .nights(2)
                .currency("AUD")
                .reservationRequired(true)
                .status("pending")
                .build());

        QuoteItem quoteItem = new QuoteItem(
                "hotel_" + hotel.getId(),
                "sku-hotel",
                150,
                1,
                150,
                "AUD",
                null,
                null,
                "Harbour Hotel"
        );
        QuoteResp quoteResp = new QuoteResp(
                "qt_hotel_123",
                OffsetDateTime.now().plusHours(1),
                "AUD",
                "quoted",
                List.of(quoteItem)
        );
        when(bookingApiClient.postQuote(any())).thenReturn(quoteResp);

        TripBookingQuote result = bookingService.quoteSingleItem(preference.getTripId(), "hotel", hotel.getId());

        assertThat(result.getQuoteToken()).isEqualTo("qt_hotel_123");
        assertThat(result.getTripId()).isEqualTo(preference.getTripId());
        assertThat(result.getEntityId()).isEqualTo(hotel.getId());
        assertThat(result.getStatus()).isEqualTo("quoted");
        assertThat(result.getTotalAmount()).isEqualTo(150);
        assertThat(result.getItemReference()).isEqualTo("hotel_" + hotel.getId());

        verify(bookingApiClient, times(1)).postQuote(any());
    }

    @Test
    void quoteItineraryStoresQuotesForEachItem() {
        TripTransportation transport = tripTransportationRepository.save(TripTransportation.builder()
                .tripId(preference.getTripId())
                .date(LocalDate.now().plusDays(3))
                .from("SYD")
                .to("MEL")
                .provider("Qantas Airways")
                .reservationRequired(true)
                .status("pending")
                .build());

        TripAttraction attraction = tripAttractionRepository.save(TripAttraction.builder()
                .tripId(preference.getTripId())
                .date(LocalDate.now().plusDays(4))
                .title("Opera House Tour")
                .location("Sydney")
                .reservationRequired(true)
                .status("pending")
                .build());

        QuoteItem transportItem = new QuoteItem(
                "transport_" + transport.getId(),
                "sku-flight",
                150,
                2,
                300,
                "AUD",
                null,
                "Qantas Airways",
                null
        );
        QuoteItem attractionItem = new QuoteItem(
                "attraction_" + attraction.getId(),
                "sku-tour",
                60,
                2,
                120,
                "AUD",
                60,
                null,
                null
        );

        ItineraryQuoteResp itineraryQuoteResp = new ItineraryQuoteResp(
                "iti_qt_456",
                OffsetDateTime.now().plusHours(2),
                "AUD",
                "quoted",
                List.of(transportItem, attractionItem)
        );
        when(bookingApiClient.postItineraryQuote(any())).thenReturn(itineraryQuoteResp);

        ItineraryQuoteResp response = bookingService.quoteItinerary(preference.getTripId());

        assertThat(response.quoteToken()).isEqualTo("iti_qt_456");

        List<TripBookingQuote> quotes = tripBookingQuoteRepository.findByTripId(preference.getTripId());
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
                .tripId(preference.getTripId())
                .date(LocalDate.now().plusDays(3))
                .from("SYD")
                .to("MEL")
                .provider("Qantas Airways")
                .reservationRequired(true)
                .status("pending")
                .build());

        QuoteItem transportItem = new QuoteItem(
                "transport_" + transport.getId(),
                "sku-flight",
                150,
                2,
                300,
                "AUD",
                null,
                "Qantas Airways",
                null
        );
        ItineraryQuoteResp itineraryQuoteResp = new ItineraryQuoteResp(
                "iti_qt_789",
                OffsetDateTime.now().plusHours(2),
                "AUD",
                "quoted",
                List.of(transportItem)
        );
        when(bookingApiClient.postItineraryQuote(any())).thenReturn(itineraryQuoteResp);
        bookingService.quoteItinerary(preference.getTripId());

        ConfirmResp confirmResponse = new ConfirmResp(
                "iti_qt_789",
                "confirmed",
                "voucher123",
                "inv456",
                OffsetDateTime.now(),
                List.of(new ConfirmResp.ConfirmedItem("transport_" + transport.getId(), "confirmed"))
        );
        when(bookingApiClient.postConfirm(any(), any())).thenReturn(confirmResponse);

        ConfirmResp confirmResp = bookingService.confirmBooking("iti_qt_789", List.of("transport_" + transport.getId()));

        assertThat(confirmResp.status()).isEqualTo("confirmed");

        TripBookingQuote quote = tripBookingQuoteRepository.findByQuoteTokenAndItemReference("iti_qt_789", "transport_" + transport.getId())
                .orElseThrow();
        assertThat(quote.getStatus()).isEqualTo("confirmed");

        TripTransportation updatedTransport = tripTransportationRepository.findById(transport.getId()).orElseThrow();
        assertThat(updatedTransport.getStatus()).isEqualTo("confirmed");

        verify(bookingApiClient, times(1)).postConfirm(any(), any());
    }
}
