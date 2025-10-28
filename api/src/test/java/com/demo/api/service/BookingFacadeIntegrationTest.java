package com.demo.api.service;

import com.demo.api.client.BookingClient;
import com.demo.api.dto.booking.ItineraryQuoteItem;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteReqItem;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteItem;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.exception.ConflictException;
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
import com.demo.api.utils.AwsS3Utils;
import com.demo.api.utils.SendGridUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking_facade;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
        "booking.base-url=http://localhost:9999",
        "aws.accesskeyId=dummy",
        "aws.secretAccessKey=dummy",
        "aws.region=ap-southeast-2",
        "aws.s3.bucket-name=dummy",
        "aws.s3.dir-name=dummy",
        "aws.s3.cdn=dummy",
        "sendgrid.api-key=dummy",
        "sendgrid.from=test@example.com",
        "spring.ai.openai.api-key=",
        "openweather.api.key=dummy-key"
})
class BookingFacadeIntegrationTest {

    @Autowired
    private BookingFacade bookingFacade;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripTransportationRepository tripTransportationRepository;

    @Autowired
    private TripHotelRepository tripHotelRepository;

    @Autowired
    private TripAttractionRepository tripAttractionRepository;

    @Autowired
    private TripBookingQuoteRepository tripBookingQuoteRepository;

    @MockBean
    private BookingClient bookingClient;

    @MockBean
    private AwsS3Utils awsS3Utils;

    @MockBean
    private SendGridUtils sendGridUtils;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(bookingClient, awsS3Utils, sendGridUtils);
    }

    @Test
    void quote_singleItem_persistsQuoteAndUpdatesEntity() {
        Trip trip = createTrip(2, "AUD");
        TripTransportation transport = tripTransportationRepository.save(
                TripTransportation.builder()
                        .tripId(trip.getId())
                        .date(LocalDate.of(2025, 3, 15))
                        .from("Sydney")
                        .to("Tokyo")
                        .provider("Qantas")
                        .ticketType("economy")
                        .reservationRequired(true)
                        .price(1200)
                        .currency("AUD")
                        .build()
        );

        QuoteItem quoteItem = new QuoteItem(
                "TP_SKU",
                BigDecimal.valueOf(1200),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1200),
                "AUD",
                Map.of("people", 2),
                "policy"
        );
        QuoteResp quoteResp = new QuoteResp("VCH-123", "INV-456", List.of(quoteItem));
        when(bookingClient.quote(any())).thenReturn(quoteResp);

        QuoteReq request = new QuoteReq(
                "transportation",
                "AUD",
                2,
                Map.of("dummy", "value"),
                trip.getId(),
                transport.getId(),
                "transport_" + transport.getId()
        );

        QuoteResp response = bookingFacade.quote(request, "integration-user");

        assertEquals("VCH-123", response.voucherCode());
        assertEquals("INV-456", response.invoiceId());
        assertEquals(1, response.items().size());
        assertEquals(BigDecimal.valueOf(1200), response.items().get(0).total());

        TripBookingQuote stored = tripBookingQuoteRepository
                .findByTripIdAndEntityIdAndProductType(trip.getId(), transport.getId(), "transportation")
                .orElseThrow();
        assertEquals("confirm", stored.getStatus());
        assertEquals(Integer.valueOf(1200), stored.getTotalAmount());
        assertEquals("transport_" + transport.getId(), stored.getItemReference());
        assertNotNull(stored.getRawResponse());

        TripTransportation updatedTransport = tripTransportationRepository.findById(transport.getId()).orElseThrow();
        assertEquals("confirm", updatedTransport.getStatus());
        verify(bookingClient).quote(any());
    }

    @Test
    void prepareItinerary_successfullyPersistsQuotesForAllItems() {
        Trip trip = createTrip(3, "JPY");
        TripTransportation transport = tripTransportationRepository.save(
                TripTransportation.builder()
                        .tripId(trip.getId())
                        .date(LocalDate.of(2025, 5, 10))
                        .from("Sydney")
                        .to("Osaka")
                        .provider("ANA")
                        .ticketType("business")
                        .reservationRequired(true)
                        .price(1500)
                        .currency("JPY")
                        .build()
        );
        TripHotel hotel = tripHotelRepository.save(
                TripHotel.builder()
                        .tripId(trip.getId())
                        .date(LocalDate.of(2025, 5, 11))
                        .hotelName("Osaka Bay Hotel")
                        .roomType("Suite")
                        .nights(2)
                        .reservationRequired(true)
                        .price(800)
                        .currency("JPY")
                        .build()
        );

        QuoteItem transportQuoteItem = new QuoteItem(
                "TP_SKU",
                BigDecimal.valueOf(1500),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1500),
                "JPY",
                Map.of(),
                "policy"
        );
        QuoteItem hotelQuoteItem = new QuoteItem(
                "HTL_SKU",
                BigDecimal.valueOf(800),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(800),
                "JPY",
                Map.of(),
                "policy"
        );
        ItineraryQuoteResp itineraryQuoteResp = new ItineraryQuoteResp(
                "VCH-ITI",
                "INV-ITI",
                "JPY",
                List.of(
                        new ItineraryQuoteItem(
                                "transport_" + transport.getId(),
                                "transport",
                                3,
                                BigDecimal.valueOf(1500),
                                BigDecimal.ZERO,
                                List.of(transportQuoteItem)
                        ),
                        new ItineraryQuoteItem(
                                "hotel_" + hotel.getId(),
                                "hotel",
                                3,
                                BigDecimal.valueOf(800),
                                BigDecimal.ZERO,
                                List.of(hotelQuoteItem)
                        )
                ),
                BigDecimal.valueOf(2300),
                BigDecimal.ZERO
        );
        when(bookingClient.itineraryQuote(any())).thenReturn(itineraryQuoteResp);

        ItineraryQuoteReq request = new ItineraryQuoteReq(
                "iti_" + trip.getId(),
                "JPY",
                List.of(
                        new ItineraryQuoteReqItem(
                                "transport_" + transport.getId(),
                                "transport",
                                3,
                                Map.of(),
                                transport.getId()
                        ),
                        new ItineraryQuoteReqItem(
                                "hotel_" + hotel.getId(),
                                "hotel",
                                3,
                                Map.of(),
                                hotel.getId()
                        )
                ),
                trip.getId()
        );

        ItineraryQuoteResp response = bookingFacade.prepareItinerary(request, "integration-user");

        assertEquals("VCH-ITI", response.voucherCode());
        assertEquals(2, response.items().size());

        List<TripBookingQuote> storedQuotes = tripBookingQuoteRepository.findByTripId(trip.getId());
        assertEquals(2, storedQuotes.size());
        storedQuotes.forEach(quote -> {
            assertEquals("confirm", quote.getStatus());
            assertEquals("VCH-ITI", quote.getVoucherCode());
            assertEquals("INV-ITI", quote.getInvoiceId());
            assertTrue(quote.getRawResponse().contains("\"voucher_code\":\"VCH-ITI\""));
        });

        TripTransportation updatedTransport = tripTransportationRepository.findById(transport.getId()).orElseThrow();
        TripHotel updatedHotel = tripHotelRepository.findById(hotel.getId()).orElseThrow();
        assertEquals("confirm", updatedTransport.getStatus());
        assertEquals("confirm", updatedHotel.getStatus());
        verify(bookingClient).itineraryQuote(any());
    }

    @Test
    void quote_singleItemAlreadyConfirmed_throwsConflictException() {
        Trip trip = createTrip(2, "AUD");
        TripAttraction attraction = tripAttractionRepository.save(
                TripAttraction.builder()
                        .tripId(trip.getId())
                        .date(LocalDate.of(2025, 7, 4))
                        .status("confirm")
                        .reservationRequired(true)
                        .ticketPrice(100)
                        .currency("AUD")
                        .title("Harbour Cruise")
                        .build()
        );

        QuoteReq request = new QuoteReq(
                "attraction",
                "AUD",
                2,
                Map.of(),
                trip.getId(),
                attraction.getId(),
                "attraction_" + attraction.getId()
        );

        assertThrows(ConflictException.class, () -> bookingFacade.quote(request, "integration-user"));
        verifyNoInteractions(bookingClient);
        assertTrue(tripBookingQuoteRepository.findByTripId(trip.getId()).isEmpty());
    }

    private Trip createTrip(int people, String currency) {
        Trip trip = Trip.builder()
                .userId(100L)
                .people(people)
                .currency(currency)
                .fromCity("Sydney")
                .toCity("Tokyo")
                .build();
        return tripRepository.save(trip);
    }
}
