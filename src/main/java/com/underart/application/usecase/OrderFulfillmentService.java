package com.underart.application.usecase;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.OrderFulfillmentUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentService implements OrderFulfillmentUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderParties orderParties;
    private final ActivityLogRepositoryPort activityLogRepository;
    private final NotificationSenderPort notifications;

    @Override
    @Transactional
    public Order ship(UUID orderId, UUID sellerId, String carrier, String trackingNumber) {
        Order order = find(orderId);
        if (!orderParties.isSeller(order, sellerId)) {
            throw new ForbiddenOperationException("Only the seller can mark the order as shipped");
        }
        Order shipped = orderRepository.save(order.markShipped(carrier.trim(), trackingNumber.trim(), Instant.now()));
        String title = orderParties.artworkOf(order).map(Artwork::title).orElse("tu obra");
        notifications.send(Notification.create(shipped.winnerId(), NotificationType.ORDER_SHIPPED,
                "Your artwork is on its way", "«" + title + "» was shipped with " + shipped.carrier() + ". Tracking number: "
                        + shipped.trackingNumber() + ". Confirm delivery when it arrives.", data(shipped)));
        activityLogRepository.log("ORDER_SHIPPED", "Order " + orderId + " shipped with " + shipped.carrier(), sellerId,
                Map.of("orderId", orderId.toString(), "trackingNumber", shipped.trackingNumber()));
        return shipped;
    }

    @Override
    @Transactional
    public Order confirmDelivery(UUID orderId, UUID buyerId) {
        Order order = find(orderId);
        if (!order.winnerId().equals(buyerId)) {
            throw new ForbiddenOperationException("Only the buyer can confirm delivery");
        }
        Order delivered = orderRepository.save(order.markDelivered(Instant.now()));
        orderParties.artworkOf(order).ifPresent(artwork -> notifications.send(Notification.create(
                artwork.artistId(), NotificationType.ORDER_DELIVERED, "Delivery confirmed",
                "The buyer received «" + artwork.title() + "».", data(delivered))));
        activityLogRepository.log("ORDER_DELIVERED", "Order " + orderId + " delivered", buyerId,
                Map.of("orderId", orderId.toString()));
        return delivered;
    }

    private static Map<String, String> data(Order order) {
        return Map.of("orderId", order.id().toString(), "auctionId", order.auctionId().toString());
    }

    private Order find(UUID orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}
