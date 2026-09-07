package com.underart.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

    private static final BigDecimal FEE_RATE = new BigDecimal("0.10");

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shipping) {
        BigDecimal fee = subtotal.multiply(FEE_RATE);
        return subtotal.add(shipping).add(fee).setScale(2, RoundingMode.HALF_UP);
    }
}
