package com.demo.externalservice.controller;

import com.demo.externalservice.dto.booking.ItineraryQuoteReq;
import com.demo.externalservice.dto.booking.ItineraryQuoteResp;
import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.dto.booking.QuoteResp;
import com.demo.externalservice.service.ItineraryService;
import com.demo.externalservice.service.PricingResult;
import com.demo.externalservice.service.PricingService;
import com.demo.externalservice.service.ReferenceGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final PricingService pricingService;
    private final ItineraryService itineraryService;

    @PostMapping("/quote")
    public QuoteResp quote(@Valid @RequestBody QuoteReq request) {
        PricingResult pricingResult = pricingService.calculate(request);
        String voucher = ReferenceGenerator.generateVoucherCode();
        String invoice = ReferenceGenerator.generateInvoiceId();
        return new QuoteResp(voucher, invoice, pricingResult.items());
    }

    @PostMapping("/itinerary/quote")
    public ItineraryQuoteResp itineraryQuote(@Valid @RequestBody ItineraryQuoteReq request) {
        return itineraryService.prepareItinerary(request);
    }
}
