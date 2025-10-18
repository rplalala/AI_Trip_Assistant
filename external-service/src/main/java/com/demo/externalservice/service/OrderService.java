package com.demo.externalservice.service;

import com.demo.externalservice.exception.IdempotencyMismatchException;
import com.demo.externalservice.model.Order;
import com.demo.externalservice.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final HexFormat HEX = HexFormat.of();

    private final OrderRepository orderRepository;
    private final TokenService tokenService;

    @Transactional
    public Order confirmOrder(TokenService.QuoteTokenPayload payload,
                              String quoteToken,
                              String idempotencyKey,
                              String paymentId,
                              BigDecimal total,
                              BigDecimal fees,
                              List<String> selectedRefs) {

        String tokenHash = hashToken(quoteToken);
        String selectionValue = (selectedRefs == null || selectedRefs.isEmpty())
                ? null
                : String.join(",", selectedRefs);

        if (StringUtils.hasText(idempotencyKey)) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Order order = existing.get();
                if (!order.getQuoteTokenHash().equals(tokenHash)) {
                    throw new IdempotencyMismatchException("Idempotency key already used for different quote token");
                }
                if (!Objects.equals(order.getItinerarySelectionRefs(), selectionValue)) {
                    throw new IdempotencyMismatchException("Idempotency key already used with different item selection");
                }
                return order;
            }
        }

        Order order = Order.builder()
                .productType(payload.isItinerary() ? "itinerary" : payload.productType())
                .currency(payload.currency())
                .amount(total.setScale(2))
                .fees(fees.setScale(2))
                .status("CONFIRMED")
                .voucherCode(generateVoucherCode())
                .invoiceId(generateInvoiceId())
                .paymentId(paymentId)
                .idempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey : null)
                .quoteTokenHash(tokenHash)
                .quoteClaimsJson(tokenService.toJson(payload))
                .itineraryId(payload.isItinerary() ? payload.itineraryId() : null)
                .itinerarySelectionRefs(selectionValue)
                .build();

        return orderRepository.save(order);
    }

    private String generateVoucherCode() {
        Random random = new Random();
        return "VCH-%04X-%04X".formatted(random.nextInt(0x10000), random.nextInt(0x10000));
    }

    private String generateInvoiceId() {
        int number = new Random().nextInt(9000) + 1000;
        return "INV_%d".formatted(number);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

}
