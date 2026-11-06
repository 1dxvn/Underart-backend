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

@Service
@RequiredArgsConstructor
public class OrderFulfillmentService implements OrderFulfillmentUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderParties orderParties;
    private final ActivityLogRepositoryPort activityLogRepository;
    private final NotificationSenderPort notifications;

    @Override
    public Order ship(UUID orderId, UUID sellerId, String carrier, String trackingNumber) {
        Order order = find(orderId);
        if (!orderParties.isSeller(order, sellerId)) {
            throw new ForbiddenOperationException("Solo el vendedor puede marcar la orden como enviada");
        }
        Order shipped = orderRepository.save(order.markShipped(carrier.trim(), trackingNumber.trim(), Instant.now()));
        String title = orderParties.artworkOf(order).map(Artwork::title).orElse("tu obra");
        notifications.send(Notification.create(shipped.winnerId(), NotificationType.ORDER_SHIPPED,
                "Tu obra va en camino", "«" + title + "» salió con " + shipped.carrier() + ". Guía: "
                        + shipped.trackingNumber() + ". Confirma la entrega cuando la recibas.", data(shipped)));
        activityLogRepository.log("ORDER_SHIPPED", "Orden " + orderId + " enviada con " + shipped.carrier(), sellerId,
                Map.of("orderId", orderId.toString(), "trackingNumber", shipped.trackingNumber()));
        return shipped;
    }

    @Override
    public Order confirmDelivery(UUID orderId, UUID buyerId) {
        Order order = find(orderId);
        if (!order.winnerId().equals(buyerId)) {
            throw new ForbiddenOperationException("Solo el comprador puede confirmar la entrega");
        }
        Order delivered = orderRepository.save(order.markDelivered(Instant.now()));
        orderParties.artworkOf(order).ifPresent(artwork -> notifications.send(Notification.create(
                artwork.artistId(), NotificationType.ORDER_DELIVERED, "Entrega confirmada",
                "El comprador recibió «" + artwork.title() + "».", data(delivered))));
        activityLogRepository.log("ORDER_DELIVERED", "Orden " + orderId + " entregada", buyerId,
                Map.of("orderId", orderId.toString()));
        return delivered;
    }

    private static Map<String, String> data(Order order) {
        return Map.of("orderId", order.id().toString(), "auctionId", order.auctionId().toString());
    }

    private Order find(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
    }
}
