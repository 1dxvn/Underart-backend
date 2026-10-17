package com.underart.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Order(
        UUID id,
        UUID auctionId,
        UUID winnerId,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal platformFee,
        BigDecimal totalPaid,
        PaymentStatus paymentStatus,
        String trackingNumber,
        Instant createdAt) {

    public static final BigDecimal STANDARD_SHIPPING_COST = new BigDecimal("15.00");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.10");

    public static BigDecimal platformFeeFor(BigDecimal subtotal) {
        return subtotal.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shipping) {
        return subtotal.add(shipping).add(platformFeeFor(subtotal)).setScale(2, RoundingMode.HALF_UP);
    }

    public static Order pending(UUID auctionId, UUID winnerId, BigDecimal subtotal) {
        String tracking = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new Order(UUID.randomUUID(), auctionId, winnerId, subtotal, STANDARD_SHIPPING_COST,
                platformFeeFor(subtotal), calculateTotal(subtotal, STANDARD_SHIPPING_COST),
                PaymentStatus.PENDING, tracking, Instant.now());
    }

    public Order withPaymentStatus(PaymentStatus status) {
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid,
                status, trackingNumber, createdAt);
    }
}
