package com.demo.externalservice.service.pricing;

import com.demo.externalservice.dto.QuoteItem;
import com.demo.externalservice.dto.QuoteReq;
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

@Component("transport")
public class TransportPricing implements PricingCalculator {

    @Override
    public PricingResult calculate(QuoteReq request) {
        Map<String, Object> params = request.params();

        String mode = stringParam(params, "mode", "transport").toLowerCase();
        String from = stringParam(params, "from", "").toUpperCase();
        String to = stringParam(params, "to", "").toUpperCase();
        LocalDate travelDate = dateParam(params, "date", LocalDate.now());
        String provider = stringParam(params, "provider", "");
        String ticketType = Optional.ofNullable(stringParam(params, "ticket_type", null))
                .orElse(stringParam(params, "class", "standard")).toLowerCase();
        String departureTime = Optional.ofNullable(stringParam(params, "time", null))
                .orElse(stringParam(params, "departure_time", "00:00"));

        BigDecimal overrideTotal = decimalParam(params, "price");
        if (overrideTotal == null) {
            throw new ValidationException("Transport quote requires price parameter");
        }

        BigDecimal total = overrideTotal.setScale(0, RoundingMode.HALF_UP);
        BigDecimal unitPrice = total;
        int quantity = 1;
        BigDecimal fees = Optional.ofNullable(decimalParam(params, "fees")).orElse(BigDecimal.ZERO);

        String fromKey = from.isBlank() ? "GEN" : from.replaceAll("\\s+", "_");
        String toKey = to.isBlank() ? "GEN" : to.replaceAll("\\s+", "_");
        String routeKey = "%s_%s".formatted(fromKey, toKey);
        String sku = "TP_%s_%s".formatted(routeKey, travelDate);

        Object reservationRequired = params.get("reservation_required");
        if (reservationRequired == null) {
            reservationRequired = Boolean.TRUE;
        }
        String status = stringParam(params, "status", "pending");
        int travellers = Math.max(1, intParam(params, "people", Math.max(1, request.partySize())));

        Map<String, Object> meta = Map.of(
                "from", from,
                "to", to,
                "date", travelDate.toString(),
                "time", departureTime,
                "provider", provider,
                "ticket_type", ticketType,
                "mode", mode,
                "status", status,
                "reservation_required", reservationRequired,
                "people", travellers
        );

        QuoteItem item = new QuoteItem(
                sku,
                unitPrice,
                quantity,
                fees,
                total,
                request.currency(),
                meta,
                "No charge until 7 days prior; 25% after."
        );

        return new PricingResult(List.of(item));
    }
}
