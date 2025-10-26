package com.demo.api.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores booking quotes returned by the external booking service for each trip item.
 */
@Entity
@Table(name = "trip_booking_quote")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripBookingQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "item_reference", nullable = false)
    private String itemReference;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "invoice_id")
    private String invoiceId;

    private String currency;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "raw_response", length = 8192)
    private String rawResponse;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Builder.Default
    private String status = "confirm";

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
        if (status == null || status.isBlank()) {
            status = "confirm";
        }
    }
}
