package com.demo.api.client;

import com.demo.api.config.BookingFeignConfig;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "bookingService",
        url = "${booking.base-url}",
        configuration = BookingFeignConfig.class
)
public interface BookingClient {

    @PostMapping("/quote")
    QuoteResp quote(@RequestBody QuoteReq request);

    @PostMapping("/itinerary/quote")
    ItineraryQuoteResp itineraryQuote(@RequestBody ItineraryQuoteReq request);
}
