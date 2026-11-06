package com.underart.application.usecase;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.OrderFulfillmentUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
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

    @Override
    public Order ship(UUID orderId, UUID sellerId, String carrier, String trackingNumber) {
        Order order = find(orderId);
        if (!orderParties.isSeller(order, sellerId)) {
            throw new ForbiddenOperationException("Solo el vendedor puede marcar la orden como enviada");
        }
        Order shipped = orderRepository.save(order.markShipped(carrier.trim(), trackingNumber.trim(), Instant.now()));
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
        activityLogRepository.log("ORDER_DELIVERED", "Orden " + orderId + " entregada", buyerId,
                Map.of("orderId", orderId.toString()));
        return delivered;
    }

    private Order find(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
    }
}
