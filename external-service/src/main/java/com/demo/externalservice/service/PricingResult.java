package com.demo.externalservice.service;

import com.demo.externalservice.dto.QuoteItem;

import java.math.BigDecimal;
import java.util.List;

public record PricingResult(List<QuoteItem> items) {

    public PricingResult {
        items = List.copyOf(items);
    }

    public BigDecimal total() {
        return items.stream()
                .map(QuoteItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal fees() {
        return items.stream()
                .map(QuoteItem::fees)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public QuoteItem primaryItem() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Quote must contain at least one item");
        }
        return items.get(0);
    }
}
