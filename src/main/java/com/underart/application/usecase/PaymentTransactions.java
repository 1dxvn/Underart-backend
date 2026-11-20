package com.underart.application.usecase;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.PaymentGatewayPort.PaymentResult;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactions {

    private final OrderRepositoryPort orders;
    private final FindUserUseCase users;
    private final CheckoutAuctionSettlement settlement;

    @Transactional
    public Order begin(UUID orderId, UUID buyerId, Function<String, String> prepareCheckout) {
        var auction = settlement.lockForOrder(orderId);
        Order order = locked(orderId);
        if (!order.winnerId().equals(buyerId)) {
            throw new ForbiddenOperationException("Only the buyer can pay this order");
        }
        Instant now = Instant.now();
        if (order.paymentStatus() == PaymentStatus.PENDING || order.paymentStatus() == PaymentStatus.FAILED) {
            auction.ifPresent(value -> settlement.requireOpen(value, now));
        }
        var buyer = users.findById(buyerId);
        if (order.paymentStatus() != PaymentStatus.PENDING && order.paymentStatus() != PaymentStatus.FAILED) {
            return order.beginPayment(buyer, now);
        }
        String currency = order.snapshot() == null ? null : order.snapshot().currency();
        String checkoutToken = prepareCheckout.apply(currency);
        return orders.save(order.beginPayment(buyer, now,
                checkoutToken == null ? buyer.paymentToken() : checkoutToken));
    }

    @Transactional
    public Completion complete(UUID orderId, String reference, PaymentResult result) {
        var auction = settlement.lockForOrder(orderId);
        Order order = locked(orderId);
        if (!reference.equals(order.payment().reference()) || order.paymentStatus() != PaymentStatus.PROCESSING) {
            return new Completion(order, false);
        }
        Order completed = orders.save(order.completePayment(result.approved(), result.transactionId()));
        if (result.approved()) auction.ifPresent(value -> settlement.close(value, completed));
        return new Completion(completed, true);
    }

    @Transactional
    public Order cancelCheckout(UUID orderId, UUID buyerId) {
        Order order = locked(orderId);
        if (!order.winnerId().equals(buyerId)) {
            throw new ForbiddenOperationException("Only the buyer can cancel this checkout");
        }
        if (!order.checkout()) throw new BusinessRuleException("This is not a checkout");
        if (order.paymentStatus() == PaymentStatus.EXPIRED) return order;
        if (order.paymentStatus() != PaymentStatus.PENDING && order.paymentStatus() != PaymentStatus.FAILED
                && order.paymentStatus() != PaymentStatus.COORDINATING) {
            throw new BusinessRuleException("A checkout with a confirmed or in-progress payment cannot be cancelled");
        }
        return orders.save(order.withPaymentStatus(PaymentStatus.EXPIRED));
    }

    @Transactional
    public void expire(UUID orderId) {
        Order order = locked(orderId);
        if ((order.paymentStatus() == PaymentStatus.PENDING || order.paymentStatus() == PaymentStatus.FAILED
                || order.paymentStatus() == PaymentStatus.COORDINATING)
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
