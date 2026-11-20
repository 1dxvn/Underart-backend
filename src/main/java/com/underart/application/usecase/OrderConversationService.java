package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.port.in.FindOrderUseCase;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.domain.port.out.OrderMessageRepository;
import com.underart.domain.port.out.OrderMessageRepository.Message;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderConversationService {
    private final FindOrderUseCase findOrder;
    private final FindUserUseCase users;
    private final OrderRepositoryPort orders;
    private final OrderMessageRepository messages;
    private final OrderParties parties;
    private final CheckoutAuctionSettlement settlement;
    private final NotificationSenderPort notifications;

    public List<Message> list(UUID orderId, UUID requester, Long after) {
        findOrder.findById(orderId, requester);
        return messages.list(orderId, after);
    }

    @Transactional
    public Message send(UUID orderId, UUID sender, String text) {
        Order order = locked(orderId);
        UUID artist = artistOf(order);
        boolean buyer = order.winnerId().equals(sender);
        if (!buyer && !artist.equals(sender)) throw new ForbiddenOperationException("No puedes acceder a este chat.");
        if (order.paymentStatus() == PaymentStatus.EXPIRED
                || (order.checkout() && (order.paymentStatus() == PaymentStatus.PENDING
                || order.paymentStatus() == PaymentStatus.FAILED))) {
            throw new BusinessRuleException("El chat requiere una reserva activa o un pedido confirmado.");
        }
        var message = messages.append(orderId, sender, text.strip());
        notify(buyer ? artist : order.winnerId(), order, NotificationType.ORDER_MESSAGE,
                "Nuevo mensaje en tu pedido", "Abre la conversación para leer el mensaje.");
        return message;
    }

    @Transactional
    public Order start(UUID orderId, UUID buyer) {
        var auction = settlement.lockForOrder(orderId);
        Order order = locked(orderId);
        requireBuyer(order, buyer);
        if (order.paymentStatus() == PaymentStatus.COORDINATING) return order;
        Instant now = Instant.now();
        auction.ifPresent(value -> settlement.requireOpen(value, now));
        Order saved = orders.save(order.coordinateExternalPayment(now));
        notify(artistOf(saved), saved, NotificationType.EXTERNAL_PAYMENT, "Coordina el pago de tu obra",
                "El comprador abrió la coordinación del pago externo. Entra al chat del pedido para acordar los detalles.");
        return saved;
    }

    @Transactional
    public Order report(UUID orderId, UUID buyer) {
        Order order = locked(orderId);
        requireBuyer(order, buyer);
        if (order.paymentStatus() == PaymentStatus.REPORTED || (order.paymentStatus() == PaymentStatus.PAID
                && order.hasExternalPayment())) return order;
        Order saved = orders.save(order.reportExternalPayment(users.findById(buyer), Instant.now()));
        notify(artistOf(saved), saved, NotificationType.EXTERNAL_PAYMENT, "El comprador reportó un pago",
                "Revisa si recibiste el dinero antes de confirmar el pago en el pedido.");
        return saved;
    }

    @Transactional
    public Order review(UUID orderId, UUID artist, boolean received) {
        var auction = settlement.lockForOrder(orderId);
        Order order = locked(orderId);
        if (!artistOf(order).equals(artist)) {
            throw new ForbiddenOperationException("Solo el artista puede confirmar que recibió el pago.");
        }
        if (received && order.paymentStatus() == PaymentStatus.PAID && order.hasExternalPayment()) return order;
        if (order.paymentStatus() != PaymentStatus.REPORTED) {
            throw new BusinessRuleException("El pedido no tiene un reporte de pago pendiente.");
        }
        PaymentStatus status = received ? PaymentStatus.PAID : Instant.now().isBefore(order.paymentDueAt())
                ? PaymentStatus.COORDINATING : PaymentStatus.EXPIRED;
        Order saved = orders.save(order.withPaymentStatus(status));
        if (received) auction.ifPresent(value -> settlement.close(value, saved));
        notify(saved.winnerId(), saved, NotificationType.EXTERNAL_PAYMENT,
                received ? "El artista confirmó el pago" : "El artista aún no recibió el pago",
                received ? "El artista confirmó que recibió tu pago y ya puede preparar la entrega."
                        : "Abre el pedido para revisar el estado del pago externo.");
        return saved;
    }

    private Order locked(UUID orderId) {
        return orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
    }

    private UUID artistOf(Order order) {
        return parties.artworkOf(order).orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada."))
                .artistId();
    }

    private void requireBuyer(Order order, UUID buyer) {
        if (!order.winnerId().equals(buyer)) throw new ForbiddenOperationException("Solo el comprador puede reportar el pago.");
    }

    private void notify(UUID recipient, Order order, NotificationType type, String title, String message) {
        notifications.send(Notification.create(recipient, type, title, message,
                Map.of("orderId", order.id().toString())));
    }
}
