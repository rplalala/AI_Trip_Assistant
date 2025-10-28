package com.demo.api.service.impl;

import com.demo.api.client.BookingClient;
import com.demo.api.dto.booking.ItineraryQuoteItem;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteReqItem;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteItem;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.exception.BookingApiException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookingServiceImplTest {

    private BookingClient bookingClient;
    private TripTransportationRepository tripTransportationRepository;
    private TripHotelRepository tripHotelRepository;
    private TripAttractionRepository tripAttractionRepository;
    private TripBookingQuoteRepository tripBookingQuoteRepository;
    private TripRepository tripRepository;
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingClient = mock(BookingClient.class);
        tripTransportationRepository = mock(TripTransportationRepository.class);
        tripHotelRepository = mock(TripHotelRepository.class);
        tripAttractionRepository = mock(TripAttractionRepository.class);
        tripBookingQuoteRepository = mock(TripBookingQuoteRepository.class);
        tripRepository = mock(TripRepository.class);
        bookingService = new BookingServiceImpl(
                bookingClient,
                tripTransportationRepository,
                tripHotelRepository,
                tripAttractionRepository,
                tripBookingQuoteRepository,
                tripRepository,
                new ObjectMapper()
        );

        when(tripBookingQuoteRepository.findByTripIdAndEntityIdAndProductType(anyLong(), anyLong(), anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void quoteSingleItem_successfulTransport_persistsQuoteAndConfirmsEntity() {
        Long tripId = 1L;
        Long transportId = 10L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .fromCity("Sydney")
                .toCity("Tokyo")
                .build();
        TripTransportation transportation = TripTransportation.builder()
                .id(transportId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 10, 26))
                .time("10:00")
                .status("pending")
                .reservationRequired(true)
                .from("Sydney, Australia")
                .to("Tokyo, Japan")
                .provider("Qantas")
                .ticketType("economy")
                .price(1000)
                .currency("AUD")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findById(transportId)).thenReturn(Optional.of(transportation));
        when(tripTransportationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tripBookingQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteItem quoteItem = new QuoteItem(
                "TP_SKU",
                BigDecimal.valueOf(1000),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1000),
                "AUD",
                Map.of("people", 2),
                "No charge"
        );
        QuoteResp quoteResp = new QuoteResp("VCH-123", "INV-456", List.of(quoteItem));
        when(bookingClient.quote(any())).thenReturn(quoteResp);

        TripBookingQuote result = bookingService.quoteSingleItem(tripId, "transport", transportId);

        ArgumentCaptor<QuoteReq> requestCaptor = ArgumentCaptor.forClass(QuoteReq.class);
        verify(bookingClient).quote(requestCaptor.capture());
        QuoteReq sentRequest = requestCaptor.getValue();
        assertEquals("transport", sentRequest.productType());
        assertEquals("AUD", sentRequest.currency());
        assertEquals(Integer.valueOf(2), sentRequest.partySize());
        assertEquals("transport_" + transportId, sentRequest.itemReference());

        ArgumentCaptor<TripBookingQuote> quoteCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository).save(quoteCaptor.capture());
        TripBookingQuote savedQuote = quoteCaptor.getValue();
        assertEquals("confirm", savedQuote.getStatus());
        assertEquals("VCH-123", savedQuote.getVoucherCode());
        assertEquals("INV-456", savedQuote.getInvoiceId());
        assertEquals("AUD", savedQuote.getCurrency());
        assertEquals(Integer.valueOf(1000), savedQuote.getTotalAmount());
        assertEquals("transportation", savedQuote.getProductType());
        assertEquals("transport_" + transportId, savedQuote.getItemReference());
        assertNotNull(savedQuote.getRawResponse());

        assertSame(savedQuote, result);

        ArgumentCaptor<TripTransportation> transportCaptor = ArgumentCaptor.forClass(TripTransportation.class);
        verify(tripTransportationRepository).save(transportCaptor.capture());
        assertEquals("confirm", transportCaptor.getValue().getStatus());
    }

    @Test
    void quoteSingleItem_missingCurrencyFallsBackToDefault() {
        Long tripId = 2L;
        Long transportId = 20L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency(null)
                .people(1)
                .fromCity("Melbourne")
                .toCity("Tokyo")
                .build();
        TripTransportation transportation = TripTransportation.builder()
                .id(transportId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 5, 10))
                .status("pending")
                .reservationRequired(true)
                .price(500)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findById(transportId)).thenReturn(Optional.of(transportation));
        when(tripTransportationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tripBookingQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteResp quoteResp = new QuoteResp(
                "VCH-890",
                "INV-000",
                List.of(new QuoteItem("SKU", BigDecimal.valueOf(500), 1, BigDecimal.ZERO, BigDecimal.valueOf(500), "AUD", Map.of(), ""))
        );
        when(bookingClient.quote(any())).thenReturn(quoteResp);

        bookingService.quoteSingleItem(tripId, "transportation", transportId);

        ArgumentCaptor<QuoteReq> requestCaptor = ArgumentCaptor.forClass(QuoteReq.class);
        verify(bookingClient).quote(requestCaptor.capture());
        assertEquals("AUD", requestCaptor.getValue().currency(), "Should default currency to AUD when entity and trip currency are absent");

        ArgumentCaptor<TripBookingQuote> quoteCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository).save(quoteCaptor.capture());
        assertEquals("AUD", quoteCaptor.getValue().getCurrency());
    }

    @Test
    void quoteSingleItem_feignException_persistsFailureAndThrowsBookingApiException() {
        Long tripId = 3L;
        Long transportId = 30L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(1)
                .fromCity("Sydney")
                .toCity("Brisbane")
                .build();
        TripTransportation transportation = TripTransportation.builder()
                .id(transportId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 3, 1))
                .status("pending")
                .reservationRequired(true)
                .price(200)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findById(transportId)).thenReturn(Optional.of(transportation));
        when(tripBookingQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Collection<String>> headers = new HashMap<>();
        Request request = Request.create(Request.HttpMethod.POST, "/quote", headers, new byte[0], StandardCharsets.UTF_8);
        FeignException feignException = new FeignException.BadRequest("bad request", request, null, headers);
        when(bookingClient.quote(any())).thenThrow(feignException);

        BookingApiException thrown = assertThrows(BookingApiException.class,
                () -> bookingService.quoteSingleItem(tripId, "transportation", transportId));
        assertTrue(thrown.getMessage().contains("Booking API call to /quote failed"));

        ArgumentCaptor<TripBookingQuote> quoteCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository).save(quoteCaptor.capture());
        TripBookingQuote saved = quoteCaptor.getValue();
        assertEquals("failed", saved.getStatus());
        assertNull(saved.getVoucherCode());
        assertNull(saved.getInvoiceId());
        assertNotNull(saved.getRawResponse());

        verify(tripTransportationRepository, never()).save(any());
    }

    @Test
    void quoteItinerary_successfulFlow_persistsQuotesAndConfirmsEntities() {
        Long tripId = 4L;
        Long transportId = 40L;
        Long hotelId = 41L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("JPY")
                .people(2)
                .fromCity("Sydney")
                .toCity("Tokyo")
                .build();

        TripTransportation transportation = TripTransportation.builder()
                .id(transportId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 10, 26))
                .time("09:00")
                .status("pending")
                .reservationRequired(true)
                .provider("Qantas")
                .ticketType("Business")
                .price(1500)
                .build();

        TripHotel hotel = TripHotel.builder()
                .id(hotelId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 10, 26))
                .status("pending")
                .reservationRequired(true)
                .nights(3)
                .price(900)
                .hotelName("Tokyo Central Hotel")
                .roomType("Suite")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(transportation));
        when(tripTransportationRepository.findById(transportId)).thenReturn(Optional.of(transportation));
        when(tripTransportationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of(hotel));
        when(tripHotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(tripHotelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of());

        when(tripBookingQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteItem transportItem = new QuoteItem(
                "TP_SKU",
                BigDecimal.valueOf(1500),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1500),
                "JPY",
                Map.of(),
                "policy"
        );
        QuoteItem hotelItem = new QuoteItem(
                "HTL_SKU",
                BigDecimal.valueOf(900),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(900),
                "JPY",
                Map.of(),
                "policy"
        );
        ItineraryQuoteItem itineraryTransport = new ItineraryQuoteItem(
                "transport_" + transportId,
                "transport",
                2,
                BigDecimal.valueOf(1500),
                BigDecimal.ZERO,
                List.of(transportItem)
        );
        ItineraryQuoteItem itineraryHotel = new ItineraryQuoteItem(
                "hotel_" + hotelId,
                "hotel",
                2,
                BigDecimal.valueOf(900),
                BigDecimal.ZERO,
                List.of(hotelItem)
        );
        ItineraryQuoteResp itineraryQuoteResp = new ItineraryQuoteResp(
                "VCH-ITI",
                "INV-ITI",
                "JPY",
                List.of(itineraryTransport, itineraryHotel),
                BigDecimal.valueOf(2400),
                BigDecimal.ZERO
        );

        when(bookingClient.itineraryQuote(any(ItineraryQuoteReq.class))).thenReturn(itineraryQuoteResp);

        bookingService.quoteItinerary(tripId);

        ArgumentCaptor<ItineraryQuoteReq> requestCaptor = ArgumentCaptor.forClass(ItineraryQuoteReq.class);
        verify(bookingClient).itineraryQuote(requestCaptor.capture());
        ItineraryQuoteReq sentRequest = requestCaptor.getValue();
        assertEquals("iti_" + tripId, sentRequest.itineraryId());
        assertEquals("JPY", sentRequest.currency());
        assertEquals(2, sentRequest.items().size());
        ItineraryQuoteReqItem transportRequest = sentRequest.items().get(0);
        assertEquals("transport", transportRequest.productType());
        assertEquals("transport_" + transportId, transportRequest.reference());

        ArgumentCaptor<TripBookingQuote> quoteCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository, times(2)).save(quoteCaptor.capture());
        List<TripBookingQuote> savedQuotes = quoteCaptor.getAllValues();
        assertEquals(2, savedQuotes.size());
        for (TripBookingQuote saved : savedQuotes) {
            assertEquals("confirm", saved.getStatus());
            assertEquals("VCH-ITI", saved.getVoucherCode());
            assertEquals("INV-ITI", saved.getInvoiceId());
            assertNotNull(saved.getRawResponse());
            assertTrue(saved.getRawResponse().contains("\"voucher_code\":\"VCH-ITI\""));
        }

        TripBookingQuote transportQuote = savedQuotes.stream()
                .filter(q -> q.getProductType().equals("transportation"))
                .findFirst()
                .orElseThrow();
        assertEquals(Integer.valueOf(1500), transportQuote.getTotalAmount());

        TripBookingQuote hotelQuote = savedQuotes.stream()
                .filter(q -> q.getProductType().equals("hotel"))
                .findFirst()
                .orElseThrow();
        assertEquals(Integer.valueOf(900), hotelQuote.getTotalAmount());

        verify(tripTransportationRepository).save(transportation);
        verify(tripHotelRepository).save(hotel);
        assertEquals("confirm", transportation.getStatus());
        assertEquals("confirm", hotel.getStatus());
    }

    @Test
    void quoteItinerary_noPendingItems_throwsIllegalArgumentException() {
        Long tripId = 5L;
        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .build();

        TripTransportation confirmedTransport = TripTransportation.builder()
                .id(50L)
                .tripId(tripId)
                .date(LocalDate.now())
                .status("confirm")
                .reservationRequired(true)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(confirmedTransport));
        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of());
        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> bookingService.quoteItinerary(tripId));
        assertTrue(thrown.getMessage().contains("No pending reservation-required items"));

        verifyNoInteractions(bookingClient);
    }

    @Test
    void quoteSingleItem_nullTripId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.quoteSingleItem(null, "hotel", 1L));
    }

    @Test
    void quoteSingleItem_entityNotInTrip_throwsIllegalArgumentException() {
        Long tripId = 6L;
        Long hotelId = 60L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .build();
        TripHotel hotel = TripHotel.builder()
                .id(hotelId)
                .tripId(999L)
                .date(LocalDate.now())
                .reservationRequired(true)
                .status("pending")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripHotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.quoteSingleItem(tripId, "hotel", hotelId));
    }

    @Test
    void quoteSingleItem_alreadyConfirmed_throwsConflictException() {
        Long tripId = 7L;
        Long attractionId = 70L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .build();
        TripAttraction attraction = TripAttraction.builder()
                .id(attractionId)
                .tripId(tripId)
                .date(LocalDate.now())
                .status("confirm")
                .reservationRequired(true)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripAttractionRepository.findById(attractionId)).thenReturn(Optional.of(attraction));

        ConflictException thrown = assertThrows(ConflictException.class,
                () -> bookingService.quoteSingleItem(tripId, "attraction", attractionId));
        assertTrue(thrown.getMessage().contains("Booking already confirmed"));

        verifyNoInteractions(bookingClient);
        verify(tripBookingQuoteRepository, never()).save(any());
    }
}
