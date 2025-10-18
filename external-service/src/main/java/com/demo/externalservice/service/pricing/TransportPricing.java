package com.demo.externalservice.service.pricing;

import com.demo.externalservice.dto.booking.QuoteItem;
import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.service.PricingResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.demo.externalservice.service.pricing.PricingSupport.*;

@Component("transport")
public class TransportPricing implements PricingCalculator {

    @Override
    public PricingResult calculate(QuoteReq request) {
        Map<String, Object> params = request.params();

        String mode = stringParam(params, "mode", "train").toLowerCase();
        String from = stringParam(params, "from", "NRT").toUpperCase();
        String to = stringParam(params, "to", "HND").toUpperCase();
        LocalDate travelDate = dateParam(params, "date", LocalDate.now());
        String travelClass = stringParam(params, "class", "economy").toLowerCase();

        Random rng = seededRandom(mode + from + to + travelDate + travelClass + request.partySize());

        int base = mode.equals("flight") ? 15000 : 7000;
        int distanceBand = Math.abs((from + to).hashCode()) % 6; // 0-5
        base += distanceBand * 1200;

        double classMultiplier = switch (travelClass) {
            case "business" -> 1.45;
            case "first" -> 1.9;
            case "premium" -> 1.25;
            default -> 1.0;
        };
        double randomness = 0.9 + (rng.nextDouble() * 0.25); // +/- ~15%
        double distanceMultiplier = 1.0 + (distanceBand * 0.12);

        BigDecimal basePrice = BigDecimal.valueOf(base);
        BigDecimal unitPrice = basePrice
                .multiply(BigDecimal.valueOf(classMultiplier))
                .multiply(BigDecimal.valueOf(distanceMultiplier))
                .multiply(BigDecimal.valueOf(randomness))
                .setScale(0, RoundingMode.HALF_UP);

        int quantity = Math.max(1, request.partySize());
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fees = subtotal
                .multiply(BigDecimal.valueOf(0.03 + (rng.nextInt(5) * 0.01)))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(fees);

        String skuPrefix = mode.equals("flight") ? "FLT" : "TRN";
        String sku = "%s_%s_%s_%s".formatted(
                skuPrefix,
                from,
                to,
                travelDate
        );

        Map<String, Object> meta = Map.of(
                "mode", mode,
                "from", from,
                "to", to,
                "travel_date", travelDate.toString(),
                "travel_class", travelClass
        );

        QuoteItem item = new QuoteItem(
                sku,
                unitPrice,
                quantity,
                fees,
                total,
                request.currency(),
                999,
                meta,
                "No charge until 7 days prior; 25% after."
        );

        return new PricingResult(List.of(item));
    }
}
