package com.demo.api.service;

import java.util.List;

import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.model.TripBookingQuote;

public interface BookingService {

    /**
     * Quotes a single trip item (e.g., a hotel, transport, or attraction) that requires reservation.
     *
     * @param tripId       The ID of the trip the item belongs to.
     * @param productType  The type of product: "transportation", "hotel", or "attraction".
     * @param entityId     The ID of the specific trip entity to quote (e.g., hotel ID).
     * @return A persisted TripBookingQuote entity that contains quote details.
     */
    TripBookingQuote quoteSingleItem(Long tripId, String productType, Long entityId);



    /**
     * Quotes all unconfirmed, reservation-required items for the given trip as a bundle.
     * Automatically determines which items to include.
     *
     * @param tripId  The ID of the trip for which a bundled itinerary quote is requested.
     * @return An ItineraryQuoteResp object containing the quoteToken and quoted item details.
     */
    ItineraryQuoteResp quoteItinerary(Long tripId);


    /**
     * Confirms a previously quoted itinerary using the quoteToken.
     * Can optionally confirm a subset of items using itemRefs.
     *
     * @param quoteToken The token issued by the Booking API during the quote phase.
     * @param itemRefs   A list of item reference IDs to confirm; if empty, confirms all.
     * @return A ConfirmResp object with confirmation details and per-item statuses.
     */
    ConfirmResp confirmBooking(String quoteToken, List<String> itemRefs);
}
