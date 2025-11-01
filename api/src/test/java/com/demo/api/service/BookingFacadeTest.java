package com.demo.api.service;

import com.demo.api.dto.booking.BookingItemResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteReqItem;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.model.TripBookingQuote;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BookingFacadeTest {

    @Mock
    private BookingService bookingService;

    private BookingFacade bookingFacade;

    @BeforeEach
    void setUp() {
        bookingFacade = new BookingFacade(bookingService, new ObjectMapper());
    }

    @Test
    void quote_whenRawResponsePresent_returnsDeserializedBody() {
        TripBookingQuote quote = new TripBookingQuote();
        quote.setRawResponse("""
                {
                  "voucher_code":"VCH",
                  "invoice_id":"INV",
                  "items":[
                    {
                      "sku":"hotel_5",
                      "unit_price":120.0,
                      "quantity":1,
                      "fees":0.0,
                      "total":120.0,
                      "currency":"AUD",
                      "cancellation_policy":"Flexible"
                    }
                  ]
                }
                """);
        when(bookingService.quoteSingleItem(10L, "hotel", 5L)).thenReturn(quote);

        QuoteReq req = new QuoteReq("hotel", "AUD", 2, Map.of(), 10L, 5L, "hotel_5");
        QuoteResp resp = bookingFacade.quote(req, "42");

        assertThat(resp.voucherCode()).isEqualTo("VCH");
        assertThat(resp.items()).hasSize(1);
    }

    @Test
    void quote_whenRawResponseMissing_buildsFallbackItem() {
        TripBookingQuote quote = new TripBookingQuote();
        quote.setProductType("hotel");
        quote.setTotalAmount(250);
        quote.setCurrency("AUD");
        quote.setStatus("confirm");
        quote.setVoucherCode("BACKUP");
        quote.setInvoiceId("INVOICE");
        when(bookingService.quoteSingleItem(10L, "hotel", 5L)).thenReturn(quote);

        QuoteReq req = new QuoteReq("hotel", "AUD", 2, Map.of(), 10L, 5L, "hotel_5");
        QuoteResp resp = bookingFacade.quote(req, "42");

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().getFirst().total()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(resp.voucherCode()).isEqualTo("BACKUP");
    }

    @Test
    void prepareItinerary_delegatesToService() {
        ItineraryQuoteResp resp = new ItineraryQuoteResp("V", "I", "AUD", List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        when(bookingService.quoteItinerary(20L)).thenReturn(resp);
        ItineraryQuoteReq req = new ItineraryQuoteReq("iti", "AUD", List.of(
                new ItineraryQuoteReqItem("ref", "hotel", 2, Map.of(), 10L)
        ), 20L);

        ItineraryQuoteResp result = bookingFacade.prepareItinerary(req, "7");

        assertThat(result.voucherCode()).isEqualTo("V");
        verify(bookingService).quoteItinerary(20L);
    }

    @Test
    void listBookings_whenUserIdInvalid_passesNullToService() {
        when(bookingService.listBookingItems(10L, null)).thenReturn(List.of());

        List<BookingItemResp> resp = bookingFacade.listBookings(10L, "not-number");

        assertThat(resp).isEmpty();
        verify(bookingService).listBookingItems(10L, null);
    }

    @Test
    void quote_whenServiceReturnsNull_throwsIllegalState() {
        QuoteReq req = new QuoteReq("hotel", "AUD", 1, Map.of(), 55L, 9L, "item");
        when(bookingService.quoteSingleItem(anyLong(), anyString(), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> bookingFacade.quote(req, "7"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Booking quote result is missing");
    }

    @Test
    void listBookings_whenUserIdBlank_parsesToNull() {
        when(bookingService.listBookingItems(5L, null)).thenReturn(List.of());

        bookingFacade.listBookings(5L, "  ");

        verify(bookingService).listBookingItems(5L, null);
    }
}
