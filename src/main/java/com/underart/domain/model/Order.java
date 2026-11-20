package com.underart.domain.model;

import com.underart.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
        Instant createdAt,
        Instant paymentDueAt,
        PurchaseSnapshot snapshot,
        PaymentDetails payment,
        boolean checkout) {

    public Order(UUID id, UUID auctionId, UUID winnerId, BigDecimal subtotal, BigDecimal shippingCost,
            BigDecimal platformFee, BigDecimal totalPaid, PaymentStatus paymentStatus, ShippingStatus shippingStatus,
            String carrier, String trackingNumber, Instant shippedAt, Instant deliveredAt, Instant createdAt,
            Instant paymentDueAt, PurchaseSnapshot snapshot, PaymentDetails payment) {
        this(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, paymentStatus, shippingStatus,
                carrier, trackingNumber, shippedAt, deliveredAt, createdAt, paymentDueAt, snapshot, payment, false);
    }

    public Order asCheckout() {
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, paymentStatus,
                shippingStatus, carrier, trackingNumber, shippedAt, deliveredAt, createdAt, paymentDueAt,
                snapshot, payment, true);
    }

    public static final BigDecimal STANDARD_SHIPPING_COST = new BigDecimal("15.00");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.10");

    public static BigDecimal platformFeeFor(BigDecimal subtotal) {
        return subtotal.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shipping) {
        return subtotal.add(shipping).add(platformFeeFor(subtotal)).setScale(2, RoundingMode.HALF_UP);
    }

    public static Order pending(UUID auctionId, UUID winnerId, BigDecimal subtotal) {
        return pending(auctionId, winnerId, subtotal, STANDARD_SHIPPING_COST);
    }

    public static Order pending(UUID auctionId, UUID winnerId, BigDecimal subtotal, BigDecimal shippingCost) {
        return new Order(UUID.randomUUID(), auctionId, winnerId, subtotal, shippingCost,
                platformFeeFor(subtotal), calculateTotal(subtotal, shippingCost),
                PaymentStatus.PENDING, ShippingStatus.PENDING, null, null, null, null, Instant.now(),
                Instant.now().plus(Duration.ofHours(48)), null, PaymentDetails.empty());
    }

    public Order withSnapshot(PurchaseSnapshot details) {
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, paymentStatus,
                shippingStatus, carrier, trackingNumber, shippedAt, deliveredAt, createdAt, paymentDueAt, details, payment, checkout);
    }

    public Order beginPayment(User buyer, Instant now) {
        if (paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.EXPIRED) {
            throw new BusinessRuleException("This order cannot be paid");
        }
        if (paymentStatus == PaymentStatus.PROCESSING) {
            if (!now.isBefore(payment.startedAt().plus(Duration.ofHours(23)))) {
                throw new BusinessRuleException("Payment needs manual reconciliation before retrying");
            }
            return this;
        }
        if (!now.isBefore(paymentDueAt)) {
            throw new BusinessRuleException("The payment deadline has passed");
        }
        if (buyer.paymentToken() == null || buyer.paymentToken().isBlank()) {
            throw new BusinessRuleException("Register a payment method before paying");
        }
        if (buyer.shippingAddress() == null || buyer.shippingAddress().isBlank()) {
            throw new BusinessRuleException("Set a shipping address before paying");
        }
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid,
                PaymentStatus.PROCESSING, shippingStatus, carrier, trackingNumber, shippedAt, deliveredAt,
                createdAt, paymentDueAt, snapshot == null ? null : snapshot.withDelivery(buyer),
                new PaymentDetails("order-" + id + "-" + UUID.randomUUID(), buyer.paymentToken(), null, now), checkout);
    }

    public Order completePayment(boolean approved, String transactionId) {
        if (paymentStatus != PaymentStatus.PROCESSING) {
            throw new BusinessRuleException("No payment is in progress");
        }
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid,
                approved ? PaymentStatus.PAID : PaymentStatus.FAILED, shippingStatus, carrier, trackingNumber,
                shippedAt, deliveredAt, createdAt, paymentDueAt, snapshot,
                new PaymentDetails(payment.reference(), null, transactionId, payment.startedAt()), checkout);
    }

    public Order withPaymentStatus(PaymentStatus status) {
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, status,
                shippingStatus, carrier, trackingNumber, shippedAt, deliveredAt, createdAt, paymentDueAt, snapshot, payment, checkout);
    }

    public Order markShipped(String newCarrier, String newTrackingNumber, Instant now) {
        if (paymentStatus != PaymentStatus.PAID) {
            throw new BusinessRuleException("The order has not been paid");
        }
        if (shippingStatus != ShippingStatus.PENDING) {
            throw new BusinessRuleException("The order has already been shipped");
        }
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, paymentStatus,
                ShippingStatus.SHIPPED, newCarrier, newTrackingNumber, now, null, createdAt, paymentDueAt, snapshot, payment, checkout);
    }

    public Order markDelivered(Instant now) {
        if (shippingStatus != ShippingStatus.SHIPPED) {
            throw new BusinessRuleException(shippingStatus == ShippingStatus.DELIVERED
                    ? "The order has already been delivered" : "The order has not been shipped");
        }
        return new Order(id, auctionId, winnerId, subtotal, shippingCost, platformFee, totalPaid, paymentStatus,
                ShippingStatus.DELIVERED, carrier, trackingNumber, shippedAt, now, createdAt, paymentDueAt, snapshot, payment, checkout);
    }
}
