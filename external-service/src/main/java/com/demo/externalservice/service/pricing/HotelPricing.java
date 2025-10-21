package com.demo.externalservice.service.pricing;

import com.demo.externalservice.dto.booking.QuoteItem;
import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.service.PricingResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.demo.externalservice.service.pricing.PricingSupport.*;

@Component("hotel")
public class HotelPricing implements PricingCalculator {

    @Override
    public PricingResult calculate(QuoteReq request) {
        Map<String, Object> params = request.params();

        String city = stringParam(params, "city", "Tokyo");
        LocalDate checkIn = dateParam(params, "check_in", LocalDate.now());
        LocalDate checkOut = dateParam(params, "check_out", checkIn.plusDays(1));
        if (!checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        nights = Math.max(1, nights);
        int stars = Math.max(1, intParam(params, "stars", 3));
        String roomType = stringParam(params, "room_type", "double");
        boolean breakfast = booleanParam(params, "breakfast", false);

        Random rng = seededRandom(city + checkIn + roomType + stars + breakfast);

        int base = 6000 + (stars * 2200);
        double roomMultiplier = switch (roomType.toLowerCase()) {
            case "suite" -> 2.6;
            case "twin" -> 1.4;
            case "single" -> 1.0;
            case "triple" -> 1.8;
            default -> 1.2;
        };
        if (breakfast) {
            roomMultiplier += 0.15;
        }
        double seasonality = 0.9 + (Math.abs(city.hashCode()) % 4) * 0.05;
        double randomFactor = 0.92 + (rng.nextDouble() * 0.2);

        BigDecimal unitPrice = BigDecimal.valueOf(base)
                .multiply(BigDecimal.valueOf(roomMultiplier))
                .multiply(BigDecimal.valueOf(seasonality))
                .multiply(BigDecimal.valueOf(randomFactor))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(nights));
        BigDecimal cityTax = BigDecimal.valueOf(200L * nights);
        BigDecimal serviceFee = subtotal.multiply(BigDecimal.valueOf(0.07)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal fees = cityTax.add(serviceFee);
        BigDecimal total = subtotal.add(fees);

        String sku = "HTL_%s_%s_%s".formatted(
                city.toUpperCase(),
                roomType.toUpperCase(),
                checkIn
        );

        Map<String, Object> meta = Map.of(
                "city", city,
                "stars", stars,
                "room_type", roomType,
                "check_in", checkIn.toString(),
                "check_out", checkOut.toString(),
                "nights", nights,
                "breakfast", breakfast,
                "hotel_id", "HTL_%s_%s".formatted(city.substring(0, Math.min(3, city.length())).toUpperCase(), Math.abs(sku.hashCode()) % 100)
        );

        QuoteItem item = new QuoteItem(
                sku,
                unitPrice,
                nights,
                fees,
                total,
                request.currency(),
                999,
                meta,
                "48h prior: full refund"
        );

        return new PricingResult(List.of(item));
    }
}
