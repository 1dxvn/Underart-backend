package com.underart.infrastructure.persistence.jpa.mapper;

import com.underart.domain.model.Order;
import com.underart.infrastructure.persistence.jpa.entity.AuctionEntity;
import com.underart.infrastructure.persistence.jpa.entity.OrderEntity;
import com.underart.infrastructure.persistence.jpa.entity.UserEntity;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static Order toDomain(OrderEntity e) {
        return new Order(e.getId(), e.getAuction().getId(), e.getWinner().getId(),
                e.getSubtotal(), e.getShippingCost(), e.getPlatformFee(), e.getTotalPaid(),
                e.getPaymentStatus(), e.getShippingStatus(), e.getCarrier(), e.getTrackingNumber(), e.getShippedAt(),
                e.getDeliveredAt(), e.getCreatedAt());
    }

    public static OrderEntity toEntity(Order o) {
        return OrderEntity.builder()
                .id(o.id())
                .auction(AuctionEntity.builder().id(o.auctionId()).build())
                .winner(UserEntity.builder().id(o.winnerId()).build())
                .subtotal(o.subtotal())
                .shippingCost(o.shippingCost())
                .platformFee(o.platformFee())
                .totalPaid(o.totalPaid())
                .paymentStatus(o.paymentStatus())
                .shippingStatus(o.shippingStatus())
                .carrier(o.carrier())
                .trackingNumber(o.trackingNumber())
                .shippedAt(o.shippedAt())
                .deliveredAt(o.deliveredAt())
                .createdAt(o.createdAt())
                .build();
    }
}
