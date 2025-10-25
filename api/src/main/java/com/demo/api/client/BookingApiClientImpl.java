package com.demo.api.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.demo.api.dto.booking.ConfirmReq;
import com.demo.api.dto.booking.ConfirmResp;
import com.demo.api.dto.booking.ItineraryQuoteReq;
import com.demo.api.dto.booking.ItineraryQuoteResp;
import com.demo.api.dto.booking.QuoteReq;
import com.demo.api.dto.booking.QuoteResp;
import com.demo.api.exception.BookingApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BookingApiClientImpl implements BookingApiClient {

    private static final Logger log = LoggerFactory.getLogger(BookingApiClientImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public BookingApiClientImpl(RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                @Value("${booking.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public QuoteResp postQuote(QuoteReq request) {
        Assert.notNull(request, "Quote request must not be null");
        log.debug("Sending booking quote request for productType={}", request.productType());
        return executePost("/quote", request, QuoteResp.class, null);
    }

    @Override
    public ItineraryQuoteResp postItineraryQuote(ItineraryQuoteReq request) {
        Assert.notNull(request, "Itinerary quote request must not be null");
        log.debug("Sending itinerary quote request itineraryId={}", request.itineraryId());
        return executePost("/itinerary/quote", request, ItineraryQuoteResp.class, null);
    }

    @Override
    public ConfirmResp postConfirm(ConfirmReq request, String idempotencyKey) {
        Assert.notNull(request, "Confirm request must not be null");
        log.debug("Sending booking confirm request for quoteToken={}", request.quoteToken());
        return executePost("/confirm", request, ConfirmResp.class, idempotencyKey);
    }

    private <T> T executePost(String path, Object payload, Class<T> responseType, String idempotencyKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                headers.add("Idempotency-Key", idempotencyKey);
            }
            String body = payload != null ? objectMapper.writeValueAsString(payload) : null;
            if (log.isTraceEnabled()) {
                log.trace("Booking API POST {} payload={}", path, body);
            }
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<T> response = restTemplate.exchange(baseUrl + path, HttpMethod.POST, requestEntity, responseType);
            log.info("Booking API {} succeeded with status {}", path, response.getStatusCode());
            return response.getBody();
        } catch (RestClientResponseException ex) {
            throw toBookingApiException(path, ex);
        } catch (JsonProcessingException ex) {
            throw new BookingApiException("Failed to serialize booking request body", null, null, ex);
        } catch (Exception ex) {
            throw new BookingApiException("Unexpected error calling booking service", null, null, ex);
        }
    }

    private BookingApiException toBookingApiException(String path, RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        String message = String.format("Booking API call to %s failed with status %s", path, ex.getStatusCode());
        log.error("{} body={}", message, responseBody, ex);
        return new BookingApiException(message, ex.getStatusCode(), responseBody, ex);
    }
}
