package com.underart.application.usecase;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.FindOrderUseCase;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindOrderService implements FindOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderParties orderParties;

    @Override
    public Order findById(UUID orderId, UUID requesterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        if (!order.winnerId().equals(requesterId) && !orderParties.isSeller(order, requesterId)) {
            throw new ForbiddenOperationException("No tienes acceso a esta orden");
        }
        return order;
    }
}
