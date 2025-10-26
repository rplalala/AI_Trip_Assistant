package com.demo.api.service;

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
     * @return An ItineraryQuoteResp object containing voucher/invoice references and quoted item details.
     */
    ItineraryQuoteResp quoteItinerary(Long tripId);


}
