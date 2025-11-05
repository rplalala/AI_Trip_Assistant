package com.demo.externalservice.service;

import com.demo.externalservice.dto.QuoteReq;
import com.demo.externalservice.service.pricing.PricingCalculator;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class PricingService {

    private final Map<String, PricingCalculator> calculatorsByType;

    public PricingService(Map<String, PricingCalculator> calculatorsByType) {
        this.calculatorsByType = calculatorsByType;
    }

    public PricingResult calculate(QuoteReq request) {
        String key = request.productType().toLowerCase(Locale.ROOT);
        PricingCalculator calculator = calculatorsByType.get(key);
        if (calculator == null) {
            throw new ValidationException("Unsupported product_type: " + request.productType());
        }
        return calculator.calculate(request);
    }
}
