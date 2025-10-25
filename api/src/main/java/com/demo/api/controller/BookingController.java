package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.booking.*;
import com.demo.api.service.BookingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final BookingFacade bookingFacade;

    @PostMapping("/quote")
    public ApiRespond<QuoteResp> quote(@Valid @RequestBody QuoteReq request, Authentication authentication) {
        return ApiRespond.success(bookingFacade.quote(request, authentication.getName()));
    }

    @PostMapping("/itinerary/quote")
    public ApiRespond<ItineraryQuoteResp> prepareItinerary(
            @Valid @RequestBody ItineraryQuoteReq request,
            Authentication authentication
    ) {
        return ApiRespond.success(bookingFacade.prepareItinerary(request, authentication.getName()));
    }

    @PostMapping("/confirm")
    public ApiRespond<ConfirmResp> confirm(
            @Valid @RequestBody ConfirmReq request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiRespond.success(bookingFacade.confirm(request, authentication.getName(), idempotencyKey));
    }
}
