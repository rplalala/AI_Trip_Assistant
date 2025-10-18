package com.demo.api.controller;

import com.demo.api.dto.booking.ConfirmReq;
import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.service.BookingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final BookingFacade bookingFacade;

    @PostMapping("/quote")
    public QuoteResp quote(@Valid @RequestBody QuoteReq request, Authentication authentication) {
        return bookingFacade.quote(request, authentication.getName());
    }

    @PostMapping("/itinerary/quote")
    public ItineraryQuoteResp prepareItinerary(
            @Valid @RequestBody ItineraryQuoteReq request,
            Authentication authentication
    ) {
        return bookingFacade.prepareItinerary(request, authentication.getName());
    }

    @PostMapping("/confirm")
    public ConfirmResp confirm(
            @Valid @RequestBody ConfirmReq request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return bookingFacade.confirm(request, authentication.getName(), idempotencyKey);
    }
}
