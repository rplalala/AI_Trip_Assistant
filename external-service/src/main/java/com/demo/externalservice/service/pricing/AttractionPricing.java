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

@Component("attraction")
public class AttractionPricing implements PricingCalculator {

    @Override
    public PricingResult calculate(QuoteReq request) {
        Map<String, Object> params = request.params();

        String city = stringParam(params, "city", "Tokyo");
        String name = stringParam(params, "name", "Attraction");
        LocalDate date = dateParam(params, "date", LocalDate.now());
        String session = stringParam(params, "session", "10:00");

        Random rng = seededRandom(city + name + session + request.partySize());

        int base = 3800 + (Math.abs(name.hashCode()) % 1200);
        BigDecimal basePrice = BigDecimal.valueOf(base);
        double sessionMultiplier = session.startsWith("1") ? 1.1 : 1.0;
        double popularityMultiplier = 1.0 + (Math.abs(name.hashCode()) % 5) * 0.05;
        double randomFactor = 0.95 + (rng.nextDouble() * 0.15);

        BigDecimal unitPrice = basePrice
                .multiply(BigDecimal.valueOf(sessionMultiplier))
                .multiply(BigDecimal.valueOf(popularityMultiplier))
                .multiply(BigDecimal.valueOf(randomFactor))
                .setScale(0, RoundingMode.HALF_UP);

        int quantity = Math.max(1, request.partySize());
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fees = BigDecimal.valueOf(200L * quantity);
        BigDecimal total = subtotal.add(fees);

        String sku = "ATN_%s_%s_%s".formatted(
                city.toUpperCase(),
                session.replace(":", ""),
                date
        );

        Map<String, Object> meta = Map.of(
                "city", city,
                "name", name,
                "date", date.toString(),
                "session", session
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
                "Cancellations up to 24h prior receive 80% refund"
        );

        return new PricingResult(List.of(item));
    }
}
