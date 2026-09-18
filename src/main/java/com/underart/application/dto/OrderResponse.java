package com.underart.application.dto;

import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
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

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.id(), o.auctionId(), o.winnerId(), o.subtotal(), o.shippingCost(),
                o.platformFee(), o.totalPaid(), o.paymentStatus(), o.trackingNumber(), o.createdAt());
    }
}
