package com.demo.externalservice.controller;

import com.demo.externalservice.dto.booking.ConfirmReq;
import com.demo.externalservice.dto.booking.ConfirmResp;
import com.demo.externalservice.dto.booking.ItineraryQuoteReq;
import com.demo.externalservice.dto.booking.ItineraryQuoteResp;
import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.dto.booking.QuoteResp;
import com.demo.externalservice.exception.QuoteExpiredException;
import com.demo.externalservice.model.Order;
import com.demo.externalservice.service.ItineraryService;
import com.demo.externalservice.service.OrderService;
import com.demo.externalservice.service.PaymentService;
import com.demo.externalservice.service.PricingResult;
import com.demo.externalservice.service.PricingService;
import com.demo.externalservice.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final PricingService pricingService;
    private final TokenService tokenService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ItineraryService itineraryService;

    @PostMapping("/quote")
    public QuoteResp quote(@Valid @RequestBody QuoteReq request) {
        PricingResult pricingResult = pricingService.calculate(request);
        TokenService.SignedQuote signedQuote = tokenService.signQuote(request, pricingResult);
        return new QuoteResp(signedQuote.token(), signedQuote.expiresAt(), pricingResult.items());
    }

    @PostMapping("/itinerary/quote")
    public ItineraryQuoteResp itineraryQuote(@Valid @RequestBody ItineraryQuoteReq request) {
        return itineraryService.prepareItinerary(request);
    }

    @PostMapping("/confirm")
    public ConfirmResp confirm(
            @Valid @RequestBody ConfirmReq request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        TokenService.QuoteTokenPayload payload = tokenService.verifyQuote(request.quoteToken());

        if (!payload.isItinerary() && request.hasItemSelection()) {
            throw new ValidationException("item_refs may only be provided for itinerary quotes");
        }

        if (payload.isItinerary()) {
            ItineraryService.ItineraryTotals totals = itineraryService.reprice(payload, request.itemRefs());
            PaymentService.PaymentResult payment = paymentService.charge(request.paymentToken(), totals.total());
            Order order = orderService.confirmOrder(
                    payload,
                    request.quoteToken(),
                    idempotencyKey,
                    payment.paymentId(),
                    totals.total(),
                    totals.fees(),
                    totals.selectedRefs()
            );
            return new ConfirmResp(order.getStatus(), order.getVoucherCode(), order.getInvoiceId());
        }

        PricingResult recalculated = pricingService.calculate(payload.toQuoteReq());
        if (recalculated.total().compareTo(payload.total()) != 0) {
            throw new QuoteExpiredException("Quote amount changed – please obtain a new quote");
        }

        PaymentService.PaymentResult payment = paymentService.charge(request.paymentToken(), payload.total());
        Order order = orderService.confirmOrder(
                payload,
                request.quoteToken(),
                idempotencyKey,
                payment.paymentId(),
                payload.total(),
                payload.fees(),
                List.of()
        );

        return new ConfirmResp(order.getStatus(), order.getVoucherCode(), order.getInvoiceId());
    }
}
