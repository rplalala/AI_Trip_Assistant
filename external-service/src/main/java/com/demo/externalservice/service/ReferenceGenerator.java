package com.demo.externalservice.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for generating voucher and invoice identifiers for mock bookings.
 */
public final class ReferenceGenerator {

    private ReferenceGenerator() {
    }

    public static String generateVoucherCode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "VCH-%04X-%04X".formatted(random.nextInt(0x10000), random.nextInt(0x10000));
    }

    public static String generateInvoiceId() {
        int number = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "INV_%d".formatted(number);
    }
}
