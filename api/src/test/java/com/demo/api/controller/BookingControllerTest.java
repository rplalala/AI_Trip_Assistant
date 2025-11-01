package com.demo.api.controller;

import com.demo.api.dto.booking.BookingItemResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteReqItem;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteItem;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.service.BookingFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingFacade bookingFacade;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingFacade))
                .build();
    }

    @DisplayName("POST /api/booking/quote delegates to BookingFacade")
    @Test
    void quote_returnsFacadeResponse() throws Exception {
        QuoteReq request = new QuoteReq(
                "hotel",
                "AUD",
                2,
                Map.of("hotel", "Skyline"),
                1L,
                10L,
                "hotel_10"
        );
        QuoteResp response = new QuoteResp(
                "VCH",
                "INV",
                List.of(new QuoteItem(
                        "SKU",
                        BigDecimal.valueOf(200),
                        1,
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(200),
                        "AUD",
                        Map.of(),
                        "policy"))
        );
        when(bookingFacade.quote(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/booking/quote")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher_code").value("VCH"));

        verify(bookingFacade).quote(request, "42");
    }

    @DisplayName("GET /api/booking lists booking items for trip")
    @Test
    void list_returnsItems() throws Exception {
        BookingItemResp resp = new BookingItemResp(
                10L,
                5L,
                "hotel",
                "Integration Hotel",
                "Deluxe room",
                "2025-05-01",
                "15:00",
                "confirm",
                true,
                500,
                "AUD",
                "https://images/hotel.png",
                Map.of("sku", "hotel_10"),
                new BookingItemResp.QuotePayload("hotel", "AUD", 2, Map.of(), 5L, 10L, "hotel_10"),
                new BookingItemResp.QuoteSummary("VCH", "INV", "confirm", "AUD", 500)
        );
        when(bookingFacade.listBookings(5L, "99")).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/booking")
                        .principal(new TestingAuthenticationToken("99", null))
                        .param("tripId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].productType").value("hotel"))
                .andExpect(jsonPath("$.data[0].quoteSummary.voucherCode").value("VCH"));

        verify(bookingFacade).listBookings(5L, "99");
    }


    @DisplayName("POST /api/booking/itinerary/quote calls facade with authentication subject")
    @Test
    void prepareItinerary_invokesFacade() throws Exception {
        ItineraryQuoteReq request = new ItineraryQuoteReq(
                "itinerary",
                "AUD",
                List.of(new ItineraryQuoteReqItem("hotel_10", "hotel", 2, Map.of(), 10L)),
                7L
        );
        ItineraryQuoteResp response = new ItineraryQuoteResp(
                "VCH-ITI",
                "INV-ITI",
                "AUD",
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        when(bookingFacade.prepareItinerary(request, "55")).thenReturn(response);

        mockMvc.perform(post("/api/booking/itinerary/quote")
                        .principal(new TestingAuthenticationToken("55", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher_code").value("VCH-ITI"));

        verify(bookingFacade).prepareItinerary(request, "55");
    }
}
