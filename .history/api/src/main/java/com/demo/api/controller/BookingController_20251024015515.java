package com.demo.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.model.TripBookingQuote;
import com.demo.api.service.BookingService;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Called when the frontend user requests a quote for a single itinerary item that requires booking.
     * Returns the latest quote details so the UI can display price and expiry information.
     */
    @PostMapping("/quote/{tripId}/{productType}/{entityId}")
    public ResponseEntity<TripBookingQuote> quoteSingleItem(@PathVariable Long tripId,
                                                            @PathVariable String productType,
                                                            @PathVariable Long entityId) {
        log.debug("Received single booking quote request tripId={}, productType={}, entityId={}", tripId, productType, entityId);
        TripBookingQuote quote = bookingService.quoteSingleItem(tripId, productType, entityId);
        return ResponseEntity.ok(quote);
    }

    /**
     * Called when the frontend user clicks "Book All" to fetch a bundled itinerary quote for all pending items.
     * This allows the UI to show combined pricing before confirmation.
     */
    @PostMapping("/itinerary/{tripId}")
    public ResponseEntity<ItineraryQuoteResp> quoteItinerary(@PathVariable Long tripId) {
        log.debug("Received itinerary booking quote request for tripId={}", tripId);
        ItineraryQuoteResp response = bookingService.quoteItinerary(tripId);
        return ResponseEntity.ok(response);
    }

    /**
     * Called after the traveller accepts a quote and wants to confirm the booking with our external provider.
     * Uses the stored quote token and optional item references to finalise the reservation.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResp> confirmBooking(@RequestBody ConfirmBookingRequest request) {
        log.debug("Received booking confirmation request for quoteToken={} with {} itemRefs",
                request.quoteToken(), request.itemRefs().size());
        ConfirmResp response = bookingService.confirmBooking(request.quoteToken(), request.itemRefs());
        return ResponseEntity.ok(response);
    }

    public record ConfirmBookingRequest(
            @JsonProperty("quote_token")
            String quoteToken,
            @JsonProperty("item_refs")
            List<String> itemRefs
    ) {
        public ConfirmBookingRequest {
            itemRefs = itemRefs == null ? List.of() : List.copyOf(itemRefs);
        }
    }
}
