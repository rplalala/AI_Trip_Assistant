package com.demo.externalservice.service;

import com.demo.externalservice.dto.ItineraryQuoteItem;
import com.demo.externalservice.dto.ItineraryQuoteReq;
import com.demo.externalservice.dto.ItineraryQuoteReqItem;
import com.demo.externalservice.dto.ItineraryQuoteResp;
import com.demo.externalservice.dto.QuoteItem;
import com.demo.externalservice.dto.QuoteReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final PricingService pricingService;

    public ItineraryQuoteResp prepareItinerary(ItineraryQuoteReq request) {
        List<PricedItem> pricedItems = new ArrayList<>(request.items().size());

        BigDecimal bundleTotal = BigDecimal.ZERO;
        BigDecimal bundleFees = BigDecimal.ZERO;

        for (ItineraryQuoteReqItem item : request.items()) {
            PricingResult pricingResult = pricingService.calculate(new QuoteReq(
                    item.productType(),
                    request.currency(),
                    item.partySize(),
                    item.params(),
                    request.tripId(),
                    item.entityId(),
                    item.reference()
            ));

            PricedItem pricedItem = new PricedItem(item, pricingResult);
            pricedItems.add(pricedItem);

            bundleTotal = bundleTotal.add(pricingResult.total());
            bundleFees = bundleFees.add(pricingResult.fees());
        }

        List<ItineraryQuoteItem> responseItems = pricedItems.stream()
                .map(this::toResponseItem)
                .toList();

        String voucher = ReferenceGenerator.generateVoucherCode();
        String invoice = ReferenceGenerator.generateInvoiceId();

        return new ItineraryQuoteResp(
                voucher,
                invoice,
                request.currency(),
                responseItems,
                bundleTotal,
                bundleFees
        );
    }

    private ItineraryQuoteItem toResponseItem(PricedItem pricedItem) {
        List<QuoteItem> quoteItems = pricedItem.pricingResult.items();
        return new ItineraryQuoteItem(
                pricedItem.request.reference(),
                pricedItem.request.productType(),
                pricedItem.request.partySize(),
                pricedItem.pricingResult.total(),
                pricedItem.pricingResult.fees(),
                quoteItems
        );
    }

    private record PricedItem(ItineraryQuoteReqItem request, PricingResult pricingResult) {
    }
}
