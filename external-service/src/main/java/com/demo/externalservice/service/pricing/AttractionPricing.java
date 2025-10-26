package com.demo.externalservice.service.pricing;

import com.demo.externalservice.dto.booking.QuoteItem;
import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.service.PricingResult;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.demo.externalservice.service.pricing.PricingSupport.*;

@Component("attraction")
public class AttractionPricing implements PricingCalculator {

    @Override
    public PricingResult calculate(QuoteReq request) {
        Map<String, Object> params = request.params();

        String title = Optional.ofNullable(stringParam(params, "title", null))
                .orElse(stringParam(params, "name", "Attraction"));
        String location = Optional.ofNullable(stringParam(params, "location", null))
                .orElse(stringParam(params, "city", ""));
        LocalDate date = dateParam(params, "date", LocalDate.now());
        String session = Optional.ofNullable(stringParam(params, "time", null))
                .orElse(stringParam(params, "session", "10:00"));
        String status = stringParam(params, "status", "pending");
        Object reservationRequired = params.get("reservation_required");
        if (reservationRequired == null) {
            reservationRequired = Boolean.TRUE;
        }

        BigDecimal overrideTotal = decimalParam(params, "price");
        if (overrideTotal == null) {
            throw new ValidationException("Attraction quote requires price parameter");
        }

        BigDecimal total = overrideTotal.setScale(0, RoundingMode.HALF_UP);
        BigDecimal unitPrice = total;
        int quantity = 1;
        BigDecimal fees = Optional.ofNullable(decimalParam(params, "fees")).orElse(BigDecimal.ZERO);
        int people = Math.max(1, intParam(params, "people", Math.max(1, request.partySize())));
        BigDecimal ticketPrice = Optional.ofNullable(decimalParam(params, "ticket_price"))
                .orElseGet(() -> decimalParam(params, "ticketPrice"));

        String sku = "ATN_%s_%s_%s".formatted(
                (location.isBlank() ? "GEN" : location.replaceAll("\\s+", "_").toUpperCase()),
                session.replace(":", ""),
                date
        );

        Map<String, Object> meta = Map.of(
                "title", title,
                "location", location,
                "date", date.toString(),
                "time", session,
                "status", status,
                "reservation_required", reservationRequired,
                "people", people,
                "ticket_price", ticketPrice
        );

        QuoteItem item = new QuoteItem(
                sku,
                unitPrice,
                quantity,
                fees,
                total,
                request.currency(),
                meta,
                "Cancellations up to 24h prior receive 80% refund"
        );

        return new PricingResult(List.of(item));
    }
}
