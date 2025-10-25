package com.demo.api.client;

import com.demo.api.dto.booking.ConfirmReq;
import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;

public interface BookingApiClient {

    QuoteResp postQuote(QuoteReq request);

    ItineraryQuoteResp postItineraryQuote(ItineraryQuoteReq request);

    ConfirmResp postConfirm(ConfirmReq request, String idempotencyKey);
}
