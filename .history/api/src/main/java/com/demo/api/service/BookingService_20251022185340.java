package com.demo.api.service.booking;

import java.util.List;

import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.model.TripBookingQuote;

public interface BookingService {

    TripBookingQuote quoteSingleItem(Long tripId, String productType, Long entityId);

    ItineraryQuoteResp quoteItinerary(Long tripId);

    ConfirmResp confirmBooking(String quoteToken, List<String> itemRefs);
}
