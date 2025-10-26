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

@Component("hotel")
public class HotelPricing implements PricingCalculator {

    @Override
    public PricingResult calculate(QuoteReq request) {
        Map<String, Object> params = request.params();

        LocalDate stayDate = dateParam(params, "date", null);
        if (stayDate == null) {
            stayDate = dateParam(params, "check_in", LocalDate.now());
        }
        int nights = Math.max(1, intParam(params, "nights", 1));
        String title = Optional.ofNullable(stringParam(params, "title", null))
                .orElse(stringParam(params, "name", "Hotel"));
        String hotelName = Optional.ofNullable(stringParam(params, "hotel_name", null))
                .orElse(stringParam(params, "hotelName", "Hotel"));
        String roomType = Optional.ofNullable(stringParam(params, "room_type", null))
                .orElse(stringParam(params, "roomType", "double"));
        String stayTime = Optional.ofNullable(stringParam(params, "time", null))
                .orElse(stringParam(params, "check_in_time", ""));

        BigDecimal overrideTotal = decimalParam(params, "price");
        if (overrideTotal == null) {
            throw new ValidationException("Hotel quote requires price parameter");
        }

        BigDecimal total = overrideTotal.setScale(0, RoundingMode.HALF_UP);
        BigDecimal unitPrice = total;
        int quantity = 1;
        BigDecimal fees = Optional.ofNullable(decimalParam(params, "fees")).orElse(BigDecimal.ZERO);

        String hotelKey = hotelName.isBlank() ? "GEN" : hotelName.replaceAll("\\s+", "_").toUpperCase();
        String sku = "HTL_%s_%s_%s".formatted(
                hotelKey,
                roomType.replaceAll("\\s+", "_").toUpperCase(),
                stayDate
        );

        Object reservationRequired = params.get("reservation_required");
        if (reservationRequired == null) {
            reservationRequired = Boolean.TRUE;
        }

        Map<String, Object> meta = Map.of(
                "room_type", roomType,
                "hotel_name", hotelName,
                "title", title,
                "date", stayDate.toString(),
                "nights", nights,
                "time", stayTime,
                "status", stringParam(params, "status", "pending"),
                "reservation_required", reservationRequired,
                "people", intParam(params, "people", request.partySize())
        );

        QuoteItem item = new QuoteItem(
                sku,
                unitPrice,
                quantity,
                fees,
                total,
                request.currency(),
                meta,
                "48h prior: full refund"
        );

        return new PricingResult(List.of(item));
    }
}
