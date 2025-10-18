package com.demo.externalservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String productType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal fees;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, unique = true, length = 24)
    private String voucherCode;

    @Column(nullable = false, unique = true, length = 20)
    private String invoiceId;

    @Column(nullable = false, length = 40)
    private String paymentId;

    @Column(length = 64, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String quoteTokenHash;

    @Lob
    @Column(nullable = false)
    private String quoteClaimsJson;

    @Column(name = "itinerary_id", length = 64)
    private String itineraryId;

    @Column(name = "selection_refs", columnDefinition = "text")
    private String itinerarySelectionRefs;

    @CreationTimestamp
    @Column(nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;
}
