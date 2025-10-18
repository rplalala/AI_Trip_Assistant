package com.demo.externalservice.service.pricing;

import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.service.PricingResult;

public interface PricingCalculator {
    PricingResult calculate(QuoteReq request);
}
