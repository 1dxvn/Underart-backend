package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.PaymentFailedException;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.port.in.PayOrderUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.domain.port.out.PaymentGatewayPort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayOrderService implements PayOrderUseCase {

    private final PaymentGatewayPort paymentGateway;
    private final ActivityLogRepositoryPort activityLog;
    private final NotificationSenderPort notifications;
    private final OrderParties parties;
    private final PaymentTransactions payments;

    @Override
    public Order pay(UUID orderId, UUID buyerId) {
        Order pending = payments.begin(orderId, buyerId);
        var result = paymentGateway.charge(pending.payment().token(), pending.totalPaid(), pending.payment().reference());
        var completion = payments.complete(orderId, pending.payment().reference(), result);
        Order order = completion.order();
        if (!order.payment().reference().equals(pending.payment().reference())) {
            throw new BusinessRuleException("Payment attempt has been superseded. Refresh the order");
        }
        if (order.paymentStatus() != PaymentStatus.PAID) {
            if (completion.changed()) {
                notifications.send(Notification.create(buyerId, NotificationType.PAYMENT_FAILED, "Payment declined",
                        "Check your payment method and try again", data(order)));
                activityLog.log("PAYMENT_FAILED", "Payment declined", buyerId, Map.of("orderId", orderId.toString()));
            }
            throw new PaymentFailedException("Payment declined: " + result.message());
        }
        if (completion.changed()) {
            notifications.send(Notification.create(buyerId, NotificationType.PAYMENT_APPROVED, "Payment approved",
                    "The artist will prepare shipping", data(order)));
            parties.artworkOf(order).ifPresent(artwork -> notifications.send(Notification.create(artwork.artistId(),
                    NotificationType.PAYMENT_APPROVED, "The buyer paid", "You can ship the artwork now", data(order))));
            activityLog.log("PAYMENT_APPROVED", "Payment approved", buyerId,
                    Map.of("orderId", orderId.toString(), "transactionId", result.transactionId()));
        }
        return order;
    }

    private Map<String, String> data(Order order) {
        return Map.of("orderId", order.id().toString(), "auctionId", order.auctionId().toString());
    }
}
