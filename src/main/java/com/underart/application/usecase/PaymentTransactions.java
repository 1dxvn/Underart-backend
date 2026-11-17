package com.underart.application.usecase;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.PaymentGatewayPort.PaymentResult;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactions {

    private final OrderRepositoryPort orders;
    private final FindUserUseCase users;

    @Transactional
    public Order begin(UUID orderId, UUID buyerId) {
        Order order = locked(orderId);
        if (!order.winnerId().equals(buyerId)) {
            throw new ForbiddenOperationException("Only the buyer can pay this order");
        }
        return orders.save(order.beginPayment(users.findById(buyerId), Instant.now()));
    }

    @Transactional
    public Completion complete(UUID orderId, String reference, PaymentResult result) {
        Order order = locked(orderId);
        if (!reference.equals(order.payment().reference()) || order.paymentStatus() != PaymentStatus.PROCESSING) {
            return new Completion(order, false);
        }
        return new Completion(orders.save(order.completePayment(result.approved(), result.transactionId())), true);
    }

    @Transactional
    public void expire(UUID orderId) {
        Order order = locked(orderId);
        if ((order.paymentStatus() == PaymentStatus.PENDING || order.paymentStatus() == PaymentStatus.FAILED)
                && !Instant.now().isBefore(order.paymentDueAt())) {
            orders.save(order.withPaymentStatus(PaymentStatus.EXPIRED));
        }
    }

    public record Completion(Order order, boolean changed) {
    }

    private Order locked(UUID id) {
        return orders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
