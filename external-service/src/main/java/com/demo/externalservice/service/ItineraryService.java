package com.demo.externalservice.service;

import com.demo.externalservice.dto.booking.ItineraryQuoteItem;
import com.demo.externalservice.dto.booking.ItineraryQuoteReq;
import com.demo.externalservice.dto.booking.ItineraryQuoteReqItem;
import com.demo.externalservice.dto.booking.ItineraryQuoteResp;
import com.demo.externalservice.dto.booking.QuoteItem;
import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.exception.QuoteExpiredException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final PricingService pricingService;
    private final TokenService tokenService;

    public ItineraryQuoteResp prepareItinerary(ItineraryQuoteReq request) {
        List<PricedItem> pricedItems = new ArrayList<>(request.items().size());
        List<TokenService.ItineraryItemSnapshot> snapshots = new ArrayList<>(request.items().size());

        BigDecimal bundleTotal = BigDecimal.ZERO;
        BigDecimal bundleFees = BigDecimal.ZERO;

        for (ItineraryQuoteReqItem item : request.items()) {
            QuoteReq quoteReq = new QuoteReq(item.productType(), request.currency(), item.partySize(), item.params());
            PricingResult pricingResult = pricingService.calculate(quoteReq);

            PricedItem pricedItem = new PricedItem(item, pricingResult);
            pricedItems.add(pricedItem);

            snapshots.add(new TokenService.ItineraryItemSnapshot(
                    item.reference(),
                    item.productType(),
                    item.partySize(),
                    item.params(),
                    pricingResult.items(),
                    pricingResult.fees(),
                    pricingResult.total()
            ));

            bundleTotal = bundleTotal.add(pricingResult.total());
            bundleFees = bundleFees.add(pricingResult.fees());
        }

        TokenService.SignedQuote signedQuote = tokenService.signItineraryQuote(request, snapshots);

        List<ItineraryQuoteItem> responseItems = pricedItems.stream()
                .map(this::toResponseItem)
                .toList();

        return new ItineraryQuoteResp(
                signedQuote.token(),
                signedQuote.expiresAt(),
                request.currency(),
                responseItems,
                bundleTotal,
                bundleFees
        );
    }

    public ItineraryTotals reprice(TokenService.QuoteTokenPayload payload, List<String> selection) {
        if (!payload.isItinerary()) {
            throw new IllegalArgumentException("Token payload is not itinerary-based");
        }

        List<String> requestedRefs = selection == null ? List.of() : List.copyOf(selection);
        List<String> effectiveRefs = requestedRefs.isEmpty() ? payload.itineraryRefs() : requestedRefs;
        if (effectiveRefs.isEmpty()) {
            throw new ValidationException("At least one itinerary item must be selected for confirmation");
        }

        Map<String, TokenService.ItineraryItemPayload> itemsByRef = payload.itineraryItemsByRef();
        Set<String> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;
        List<String> validatedRefs = new ArrayList<>(effectiveRefs.size());

        for (String ref : effectiveRefs) {
            if (!seen.add(ref)) {
                throw new ValidationException("Duplicate itinerary item reference in selection: " + ref);
            }
            TokenService.ItineraryItemPayload itemPayload = itemsByRef.get(ref);
            if (itemPayload == null) {
                throw new ValidationException("Unknown itinerary item reference: " + ref);
            }

            QuoteReq quoteReq = itemPayload.toQuoteReq(payload.currency());
            PricingResult recalculated = pricingService.calculate(quoteReq);

            if (recalculated.total().compareTo(itemPayload.total()) != 0
                    || recalculated.fees().compareTo(itemPayload.fees()) != 0) {
                throw new QuoteExpiredException("Quote amount changed – please obtain a new quote");
            }

            total = total.add(itemPayload.total());
            fees = fees.add(itemPayload.fees());
            validatedRefs.add(ref);
        }

        return new ItineraryTotals(total, fees, List.copyOf(validatedRefs));
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

    public record ItineraryTotals(BigDecimal total, BigDecimal fees, List<String> selectedRefs) {
        public ItineraryTotals {
            selectedRefs = List.copyOf(selectedRefs);
        }
    }
}
