package com.demo.api.service.impl;

import com.demo.api.client.BookingClient;
import com.demo.api.dto.booking.BookingItemResp;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

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

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
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
    void quoteSingleItem_whenSerializationFails_usesFallbackRawResponse() throws JsonProcessingException {
        Long tripId = 2_1L;
        Long transportId = 201L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("NZD")
                .people(1)
                .fromCity("Auckland")
                .toCity("Queenstown")
                .build();
        TripTransportation transportation = TripTransportation.builder()
                .id(transportId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 6, 1))
                .status("pending")
                .reservationRequired(true)
                .provider("Southern Alps Air")
                .ticketType("Economy")
                .build();

        ObjectMapper failingMapper = mock(ObjectMapper.class);
        BookingClient localBookingClient = mock(BookingClient.class);
        BookingServiceImpl serializationService = new BookingServiceImpl(
                localBookingClient,
                tripTransportationRepository,
                tripHotelRepository,
                tripAttractionRepository,
                tripBookingQuoteRepository,
                tripRepository,
                failingMapper
        );

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findById(transportId)).thenReturn(Optional.of(transportation));
        when(tripTransportationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tripBookingQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteResp response = new QuoteResp(
                "VCH-SAFE",
                "INV-SAFE",
                List.of()
        );
        when(localBookingClient.quote(any())).thenReturn(response);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("serialize error") {});

        TripBookingQuote saved = serializationService.quoteSingleItem(tripId, "transportation", transportId);
        assertEquals("VCH-SAFE", saved.getVoucherCode());
        assertEquals(response.toString(), saved.getRawResponse());
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
    void quoteItinerary_usesContextCurrencyWhenQuoteItemHasNoCurrency() {
        Long tripId = 4_1L;
        Long transportId = 401L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("USD")
                .people(3)
                .fromCity("New York")
                .toCity("Los Angeles")
                .build();
        TripTransportation transport = TripTransportation.builder()
                .id(transportId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 5, 20))
                .status("pending")
                .reservationRequired(true)
                .provider("Interstate Bus Express")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(transport));
        when(tripTransportationRepository.findById(transportId)).thenReturn(Optional.of(transport));
        when(tripTransportationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of());
        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of());
        when(tripBookingQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteItem transportItem = new QuoteItem(
                "SKU-CTX",
                BigDecimal.valueOf(480),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(480),
                null,
                Map.of(),
                "policy"
        );
        ItineraryQuoteItem itineraryTransport = new ItineraryQuoteItem(
                "transport_" + transportId,
                "transport",
                3,
                BigDecimal.valueOf(480),
                BigDecimal.ZERO,
                List.of(transportItem)
        );
        ItineraryQuoteResp response = new ItineraryQuoteResp(
                "VOUCH-CTX",
                "INV-CTX",
                "JPY",
                List.of(itineraryTransport),
                BigDecimal.valueOf(480),
                BigDecimal.ZERO
        );

        when(bookingClient.itineraryQuote(any(ItineraryQuoteReq.class))).thenReturn(response);

        bookingService.quoteItinerary(tripId);

        ArgumentCaptor<TripBookingQuote> quoteCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository).save(quoteCaptor.capture());
        TripBookingQuote saved = quoteCaptor.getValue();
        assertEquals("USD", saved.getCurrency());
        assertEquals(Integer.valueOf(480), saved.getTotalAmount());
        assertEquals("transportation", saved.getProductType());
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
    void quoteItinerary_reservationNotRequiredItemsAreIgnored() {
        Long tripId = 5_1L;
        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(1)
                .build();
        TripTransportation optionalTransport = TripTransportation.builder()
                .id(501L)
                .tripId(tripId)
                .date(LocalDate.now().plusDays(2))
                .status("pending")
                .reservationRequired(false)
                .from("SYD")
                .to("MEL")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(optionalTransport));
        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of());
        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> bookingService.quoteItinerary(tripId));
        assertTrue(thrown.getMessage().contains("No pending reservation-required items"));

        verifyNoInteractions(bookingClient);
        verify(tripTransportationRepository, never()).findById(optionalTransport.getId());
    }

    @Test
    void quoteSingleItem_nullTripId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.quoteSingleItem(null, "hotel", 1L));
    }

    @Test
    void quoteSingleItem_unsupportedProductType_throwsIllegalArgumentException() {
        Long tripId = 6_1L;
        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> bookingService.quoteSingleItem(tripId, "Cruise", 77L));
        assertTrue(thrown.getMessage().contains("Unsupported product type"));
        verify(tripBookingQuoteRepository, never()).save(any());
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

    @Test
    void quoteSingleItem_whenBookingApiFails_persistsFailureAndWraps() {
        Long tripId = 9L;
        Long hotelId = 90L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .build();
        TripHotel hotel = TripHotel.builder()
                .id(hotelId)
                .tripId(tripId)
                .date(LocalDate.now())
                .reservationRequired(true)
                .status("pending")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripHotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        TripBookingQuote existing = TripBookingQuote.builder()
                .tripId(tripId)
                .entityId(hotelId)
                .productType("hotel")
                .itemReference("hotel_" + hotelId)
                .build();
        when(tripBookingQuoteRepository.findByTripIdAndEntityIdAndProductType(tripId, hotelId, "hotel"))
                .thenReturn(Optional.of(existing));

        Request request = Request.create(Request.HttpMethod.POST, "/quote", Map.of(), new byte[0],
                StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(502)
                .request(request)
                .body("{\"error\":\"fail\"}", StandardCharsets.UTF_8)
                .build();
        FeignException feignFailure = FeignException.errorStatus("bookingClient#quote", response);
        when(bookingClient.quote(any())).thenThrow(feignFailure);

        BookingApiException thrown = assertThrows(BookingApiException.class,
                () -> bookingService.quoteSingleItem(tripId, "hotel", hotelId));
        assertTrue(thrown.getMessage().contains("/quote"));

        ArgumentCaptor<TripBookingQuote> failureCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository).save(failureCaptor.capture());
        TripBookingQuote persisted = failureCaptor.getValue();
       assertEquals("failed", persisted.getStatus());
       assertNull(persisted.getVoucherCode());
       assertEquals("hotel_" + hotelId, persisted.getItemReference());
       assertTrue(persisted.getRawResponse().contains("fail"));
   }

    @Test
    void quoteSingleItem_runtimeException_persistsFailureAndPropagates() {
        Long tripId = 10L;
        Long attractionId = 100L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .toCity("Tokyo")
                .build();
        TripAttraction attraction = TripAttraction.builder()
                .id(attractionId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 3, 15))
                .status("pending")
                .reservationRequired(true)
                .title("Skytree")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripAttractionRepository.findById(attractionId)).thenReturn(Optional.of(attraction));

        IllegalStateException failure = new IllegalStateException("boom");
        when(bookingClient.quote(any())).thenThrow(failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> bookingService.quoteSingleItem(tripId, "attraction", attractionId));
        assertSame(failure, thrown);

        ArgumentCaptor<TripBookingQuote> quoteCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository).save(quoteCaptor.capture());
        TripBookingQuote saved = quoteCaptor.getValue();
        assertEquals("failed", saved.getStatus());
        assertEquals("boom", saved.getRawResponse());
        verify(tripAttractionRepository, never()).save(any());
    }

    @Test
    void quoteSingleItem_bookingApiException_persistsFailureAndRethrows() {
        Long tripId = 10_1L;
        Long transportId = 101L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .fromCity("Sydney")
                .toCity("Brisbane")
                .build();
        TripTransportation transportation = TripTransportation.builder()
                .id(transportId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 3, 20))
                .status("pending")
                .reservationRequired(true)
                .from("Sydney")
                .to("Brisbane")
                .provider("Qantas")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTransportationRepository.findById(transportId)).thenReturn(Optional.of(transportation));

        BookingApiException apiFailure = new BookingApiException("bad request", HttpStatus.BAD_REQUEST, "{\"error\":\"bad\"}");
        when(bookingClient.quote(any())).thenThrow(apiFailure);

        BookingApiException thrown = assertThrows(BookingApiException.class,
                () -> bookingService.quoteSingleItem(tripId, "transportation", transportId));
        assertSame(apiFailure, thrown);

        ArgumentCaptor<TripBookingQuote> failureCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository).save(failureCaptor.capture());
        TripBookingQuote persisted = failureCaptor.getValue();
        assertEquals("failed", persisted.getStatus());
        assertEquals("{\"error\":\"bad\"}", persisted.getRawResponse());
        verify(tripTransportationRepository, never()).save(any());
    }

    @Test
    void quoteItinerary_whenBookingApiFails_recordsFailuresForEachEntity() {
        Long tripId = 11L;
        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        TripTransportation transport = TripTransportation.builder()
                .id(101L)
                .tripId(tripId)
                .date(LocalDate.now())
                .status("pending")
                .reservationRequired(true)
                .from("SYD")
                .to("MEL")
                .provider("Qantas")
                .build();
        TripHotel hotel = TripHotel.builder()
                .id(202L)
                .tripId(tripId)
                .date(LocalDate.now().plusDays(1))
                .status("pending")
                .reservationRequired(true)
                .hotelName("Stay Inn")
                .build();

        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(transport));
        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of(hotel));
        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of());

        when(tripTransportationRepository.findById(transport.getId())).thenReturn(Optional.of(transport));
        when(tripHotelRepository.findById(hotel.getId())).thenReturn(Optional.of(hotel));

        Request request = Request.create(Request.HttpMethod.POST, "/itinerary/quote", Map.of(), new byte[0],
                StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(503)
                .request(request)
                .body("service down", StandardCharsets.UTF_8)
                .build();
        FeignException feignFailure = FeignException.errorStatus("bookingClient#itineraryQuote", response);
        when(bookingClient.itineraryQuote(any())).thenThrow(feignFailure);

        BookingApiException thrown = assertThrows(BookingApiException.class,
                () -> bookingService.quoteItinerary(tripId));
        assertTrue(thrown.getMessage().contains("/itinerary/quote"));

        ArgumentCaptor<TripBookingQuote> failureCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository, times(2)).save(failureCaptor.capture());
        List<TripBookingQuote> failures = failureCaptor.getAllValues();
        assertEquals(2, failures.size());
        failures.forEach(quote -> {
            assertEquals("failed", quote.getStatus());
            assertNull(quote.getVoucherCode());
            assertEquals("service down", quote.getRawResponse());
        });
    }

    @Test
    void quoteItinerary_bookingApiException_recordsFailuresAndRethrows() {
        Long tripId = 11_2L;
        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(3)
                .fromCity("Sydney")
                .toCity("Cairns")
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        TripTransportation transport = TripTransportation.builder()
                .id(303L)
                .tripId(tripId)
                .date(LocalDate.now())
                .status("pending")
                .reservationRequired(true)
                .provider("Virgin")
                .from("SYD")
                .to("CNS")
                .build();
        TripAttraction attraction = TripAttraction.builder()
                .id(404L)
                .tripId(tripId)
                .date(LocalDate.now().plusDays(1))
                .status("pending")
                .reservationRequired(true)
                .title("Reef Tour")
                .ticketPrice(220)
                .build();

        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(transport));
        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of());
        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of(attraction));

        when(tripTransportationRepository.findById(transport.getId())).thenReturn(Optional.of(transport));
        when(tripAttractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

        BookingApiException apiFailure = new BookingApiException("service unavailable", HttpStatus.SERVICE_UNAVAILABLE, "{\"error\":\"busy\"}");
        when(bookingClient.itineraryQuote(any(ItineraryQuoteReq.class))).thenThrow(apiFailure);

        BookingApiException thrown = assertThrows(BookingApiException.class,
                () -> bookingService.quoteItinerary(tripId));
        assertSame(apiFailure, thrown);

        ArgumentCaptor<TripBookingQuote> failureCaptor = ArgumentCaptor.forClass(TripBookingQuote.class);
        verify(tripBookingQuoteRepository, times(2)).save(failureCaptor.capture());
        failureCaptor.getAllValues().forEach(quote -> {
            assertEquals("failed", quote.getStatus());
            assertEquals("{\"error\":\"busy\"}", quote.getRawResponse());
        });
    }

    @Test
    void listBookingItems_userMismatch_throwsAccessDenied() {
        Long tripId = 20L;
        Trip trip = Trip.builder()
                .id(tripId)
                .userId(1L)
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThrows(AccessDeniedException.class,
                () -> bookingService.listBookingItems(tripId, 2L));
    }

    @Test
    void listBookingItems_returnsSortedItemsWithMetadata() {
        Long tripId = 30L;
        Trip trip = Trip.builder()
                .id(tripId)
                .userId(7L)
                .fromCity("Sydney")
                .toCity("Tokyo")
                .currency("AUD")
                .people(2)
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        TripTransportation transport = TripTransportation.builder()
                .id(100L)
                .tripId(tripId)
                .date(LocalDate.of(2025, 1, 1))
                .time("08:00")
                .reservationRequired(true)
                .status(null)
                .to("Tokyo")
                .provider("Qantas Airways")
                .price(500)
                .build();
        TripHotel hotel = TripHotel.builder()
                .id(200L)
                .tripId(tripId)
                .date(LocalDate.of(2025, 1, 2))
                .reservationRequired(true)
                .status("pending")
                .hotelName("Harbour Hotel")
                .price(700)
                .currency("JPY")
                .build();
        TripAttraction attraction = TripAttraction.builder()
                .id(300L)
                .tripId(tripId)
                .date(LocalDate.of(2025, 1, 2))
                .time("09:00")
                .reservationRequired(true)
                .status(null)
                .location("Shinjuku")
                .ticketPrice(120)
                .build();

        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(transport));
        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of(hotel));
        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of(attraction));

        when(tripTransportationRepository.findById(transport.getId())).thenReturn(Optional.of(transport));
        when(tripHotelRepository.findById(hotel.getId())).thenReturn(Optional.of(hotel));
        when(tripAttractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

        TripBookingQuote transportQuote = TripBookingQuote.builder()
                .tripId(tripId)
                .entityId(transport.getId())
                .productType("transportation")
                .currency("AUD")
                .status("confirm")
                .totalAmount(500)
                .voucherCode("TR-VOUCH")
                .invoiceId("INV-T")
                .build();
        TripBookingQuote hotelQuote = TripBookingQuote.builder()
                .tripId(tripId)
                .entityId(hotel.getId())
                .productType("hotel")
                .currency("JPY")
                .status("confirm")
                .totalAmount(700)
                .voucherCode("HT-VOUCH")
                .invoiceId("INV-H")
                .build();
        TripBookingQuote attractionQuote = TripBookingQuote.builder()
                .tripId(tripId)
                .entityId(attraction.getId())
                .productType("attraction")
                .currency("AUD")
                .status("pending")
                .totalAmount(240)
                .voucherCode("AT-VOUCH")
                .invoiceId("INV-A")
                .build();
        when(tripBookingQuoteRepository.findByTripId(tripId))
                .thenReturn(List.of(transportQuote, hotelQuote, attractionQuote));

        List<BookingItemResp> items = bookingService.listBookingItems(tripId, trip.getUserId());
        assertEquals(3, items.size());

        BookingItemResp first = items.get(0);
        assertEquals("transportation", first.productType());
        assertEquals("2025-01-01", first.date());
        assertEquals("pending", first.status());
        assertEquals("AUD", first.currency());
        assertEquals("TR-VOUCH", first.quoteSummary().voucherCode());

        BookingItemResp second = items.get(1);
        assertEquals("attraction", second.productType());
        assertEquals("Shinjuku", second.subtitle());
        assertEquals("AT-VOUCH", second.quoteSummary().voucherCode());
        assertTrue(second.metadata().containsKey("ticketPrice"));

        BookingItemResp third = items.get(2);
        assertEquals("hotel", third.productType());
       assertEquals("Harbour Hotel", third.subtitle());
       assertEquals("JPY", third.currency());
        assertEquals("HT-VOUCH", third.quoteSummary().voucherCode());
        assertEquals("hotel_" + hotel.getId(), third.quoteRequest().itemReference());
    }

    @Test
    void quoteSingleItem_successfulAttraction_confirmsStatus() {
        Long tripId = 30_1L;
        Long attractionId = 301L;

        Trip trip = Trip.builder()
                .id(tripId)
                .currency("AUD")
                .people(2)
                .toCity("Sydney")
                .build();
        TripAttraction attraction = TripAttraction.builder()
                .id(attractionId)
                .tripId(tripId)
                .date(LocalDate.of(2025, 4, 10))
                .time("11:00")
                .status("pending")
                .reservationRequired(true)
                .title("Harbour Cruise")
                .ticketPrice(180)
                .currency("AUD")
                .imageUrl("http://image")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripAttractionRepository.findById(attractionId)).thenReturn(Optional.of(attraction));
        when(tripAttractionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tripBookingQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteItem item = new QuoteItem(
                "AT-SKU",
                BigDecimal.valueOf(360),
                1,
                BigDecimal.ZERO,
                BigDecimal.valueOf(360),
                "AUD",
                Map.of(),
                "policy"
        );
        QuoteResp response = new QuoteResp("V-AT-1", "INV-AT-1", List.of(item));
        when(bookingClient.quote(any())).thenReturn(response);

        TripBookingQuote saved = bookingService.quoteSingleItem(tripId, "attraction", attractionId);
        assertEquals("confirm", saved.getStatus());
        verify(tripAttractionRepository).save(attraction);
        assertEquals("confirm", attraction.getStatus());
    }

    @Test
    void listBookingItems_skipsEntitiesWithoutReservationRequirement() {
        Long tripId = 31L;
        Trip trip = Trip.builder()
                .id(tripId)
                .userId(9L)
                .currency("AUD")
                .people(2)
                .toCity("Brisbane")
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        TripTransportation optionalTransport = TripTransportation.builder()
                .id(310L)
                .tripId(tripId)
                .date(LocalDate.of(2025, 2, 10))
                .reservationRequired(false)
                .status("pending")
                .build();
        TripHotel hotel = TripHotel.builder()
                .id(311L)
                .tripId(tripId)
                .date(LocalDate.of(2025, 2, 11))
                .reservationRequired(true)
                .status("pending")
                .hotelName("River View Hotel")
                .build();

        when(tripTransportationRepository.findByTripId(tripId)).thenReturn(List.of(optionalTransport));
        when(tripHotelRepository.findByTripId(tripId)).thenReturn(List.of(hotel));
        when(tripAttractionRepository.findByTripId(tripId)).thenReturn(List.of());

        when(tripHotelRepository.findById(hotel.getId())).thenReturn(Optional.of(hotel));
        when(tripBookingQuoteRepository.findByTripId(tripId)).thenReturn(List.of());

        List<BookingItemResp> items = bookingService.listBookingItems(tripId, trip.getUserId());
        assertEquals(1, items.size());
        assertEquals("hotel", items.getFirst().productType());

        verify(tripTransportationRepository, never()).findById(optionalTransport.getId());
    }

    @Test
    void helperMethods_coverEdgeCases() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(bookingService, "normalizeProductType", ""));
        String normalized = ReflectionTestUtils.invokeMethod(bookingService, "normalizeProductType", " Cruise ");
        assertEquals("cruise", normalized);

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(bookingService, "mapToApiProductType", ""));
        String mapped = ReflectionTestUtils.invokeMethod(bookingService, "mapToApiProductType", "transportation");
        assertEquals("transport", mapped);

        Boolean shouldSkip = ReflectionTestUtils.invokeMethod(bookingService, "shouldQuote", Boolean.FALSE, "pending");
        assertFalse(shouldSkip);

        String serialized = ReflectionTestUtils.invokeMethod(bookingService, "serializeSafely", new Object() {
            @Override
            public String toString() {
                return "fallback";
            }
        });
        assertEquals("fallback", serialized);

        Integer total = ReflectionTestUtils.invokeMethod(bookingService, "computeTotalAmount", (List<QuoteItem>) null);
        assertNull(total);
        Integer rounded = ReflectionTestUtils.invokeMethod(bookingService, "toIntegerAmount", BigDecimal.valueOf(10.6));
        assertEquals(11, rounded);

        Integer nullAmount = ReflectionTestUtils.invokeMethod(bookingService, "toIntegerAmount", (BigDecimal) null);
        assertNull(nullAmount);

        ItineraryQuoteItem itineraryItem = new ItineraryQuoteItem(
                "ref",
                "transport",
                1,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        );
        Class<?> ctxClass = Class.forName("com.demo.api.service.impl.BookingServiceImpl$ItineraryItemContext");
        var ctxConstructor = ctxClass.getDeclaredConstructor(String.class, Long.class, String.class, Map.class, String.class, Integer.class);
        ctxConstructor.setAccessible(true);
        Object ctx = ctxConstructor.newInstance("transportation", 1L, "ref", Map.of(), null, 1);
        String currency = ReflectionTestUtils.invokeMethod(bookingService, "resolveItemCurrency", itineraryItem, ctx, "AUD");
        assertEquals("AUD", currency);

        String reference = ReflectionTestUtils.invokeMethod(bookingService, "buildReference", "cruise", 99L);
        assertEquals("cruise_99", reference);

        String inferredTrain = ReflectionTestUtils.invokeMethod(bookingService, "inferTransportationMode", "National Rail");
        assertEquals("train", inferredTrain);
        String inferredBus = ReflectionTestUtils.invokeMethod(bookingService, "inferTransportationMode", "Greyhound");
        assertEquals("bus", inferredBus);
        String inferredDefault = ReflectionTestUtils.invokeMethod(bookingService, "inferTransportationMode", "");
        assertEquals("bus", inferredDefault);
    }
}

