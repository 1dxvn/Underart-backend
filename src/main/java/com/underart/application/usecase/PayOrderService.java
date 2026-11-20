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
import com.underart.domain.port.out.PaymentGatewayPort.CheckoutCard;
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
        return pay(orderId, buyerId, null);
    }

    @Override
    public Order pay(UUID orderId, UUID buyerId, CheckoutCard card) {
        Order pending = payments.begin(orderId, buyerId, currency -> paymentGateway.prepareCheckout(card, currency));
        var result = pending.snapshot() == null
                ? paymentGateway.charge(pending.payment().token(), pending.totalPaid(), pending.payment().reference())
                : paymentGateway.charge(pending.payment().token(), pending.totalPaid(), pending.payment().reference(),
                        pending.snapshot().currency());
        var completion = payments.complete(orderId, pending.payment().reference(), result);
        Order order = completion.order();
        if (!order.payment().reference().equals(pending.payment().reference())) {
            throw new BusinessRuleException("Payment attempt has been superseded. Refresh the order");
        }
        if (order.paymentStatus() != PaymentStatus.PAID) {
            if (completion.changed()) {
                notifications.send(Notification.create(buyerId, NotificationType.PAYMENT_FAILED, "Pago rechazado",
                        "Revisa tu medio de pago e inténtalo de nuevo.", data(order)));
                activityLog.log("PAYMENT_FAILED", "Pago rechazado", buyerId, Map.of("orderId", orderId.toString()));
            }
            throw new PaymentFailedException("Pago rechazado: " + result.message());
        }
        if (completion.changed()) {
            notifications.send(Notification.create(buyerId, NotificationType.PAYMENT_APPROVED, "Pago aprobado",
                    "El artista preparará el envío.", data(order)));
            parties.artworkOf(order).ifPresent(artwork -> notifications.send(Notification.create(artwork.artistId(),
                    NotificationType.PAYMENT_APPROVED, "El comprador pagó", "Ya puedes enviar la obra.", data(order))));
            activityLog.log("PAYMENT_APPROVED", "Pago aprobado", buyerId,
                    Map.of("orderId", orderId.toString(), "transactionId", result.transactionId()));
        }
        return order;
    }

    private Map<String, String> data(Order order) {
        return Map.of("orderId", order.id().toString(), "auctionId", order.auctionId().toString());
    }
}
