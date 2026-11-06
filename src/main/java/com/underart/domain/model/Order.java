package com.underart.domain.model;

import com.underart.domain.exception.BusinessRuleException;
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
        ShippingStatus shippingStatus,
        String carrier,
        String trackingNumber,
        Instant shippedAt,
        Instant deliveredAt,
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
        return new Order(UUID.randomUUID(), auctionId, winnerId, subtotal, STANDARD_SHIPPING_COST,
                platformFeeFor(subtotal), calculateTotal(subtotal, STANDARD_SHIPPING_COST),
                PaymentStatus.PENDING, ShippingStatus.PENDING, null, null, null, null, Instant.now());
    }

    public Order withPaymentStatus(PaymentStatus status) {
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, status,
                shippingStatus, carrier, trackingNumber, shippedAt, deliveredAt, createdAt);
    }

    public Order markShipped(String newCarrier, String newTrackingNumber, Instant now) {
        if (paymentStatus != PaymentStatus.PAID) {
            throw new BusinessRuleException("La orden aún no está pagada");
        }
        if (shippingStatus != ShippingStatus.PENDING) {
            throw new BusinessRuleException("La orden ya fue enviada");
        }
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, paymentStatus,
                ShippingStatus.SHIPPED, newCarrier, newTrackingNumber, now, null, createdAt);
    }

    public Order markDelivered(Instant now) {
        if (shippingStatus != ShippingStatus.SHIPPED) {
            throw new BusinessRuleException(shippingStatus == ShippingStatus.DELIVERED
                    ? "La orden ya fue entregada" : "La orden aún no fue enviada");
        }
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, paymentStatus,
                ShippingStatus.DELIVERED, carrier, trackingNumber, shippedAt, now, createdAt);
    }
}
