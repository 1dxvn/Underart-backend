package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.PaymentFailedException;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.model.User;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.PaymentGatewayPort;
import com.underart.domain.port.out.PaymentGatewayPort.PaymentResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PayOrderServiceTest {

    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final FindUserUseCase users = mock(FindUserUseCase.class);
    private final PaymentGatewayPort gateway = mock(PaymentGatewayPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final NotificationSenderPort notifications = mock(NotificationSenderPort.class);
    private final OrderParties parties = mock(OrderParties.class);
    private final PayOrderService service = new PayOrderService(gateway, log, notifications, parties, new PaymentTransactions(orders, users));
    private final UUID buyerId = UUID.randomUUID();
    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.pending(UUID.randomUUID(), buyerId, new BigDecimal("100.00"));
        java.util.concurrent.atomic.AtomicReference<Order> stored = new java.util.concurrent.atomic.AtomicReference<>(order);
        when(orders.findByIdForUpdate(order.id())).thenAnswer(i -> Optional.of(stored.get()));
        when(orders.save(any(Order.class))).thenAnswer(i -> {
            Order updated = i.getArgument(0);
            stored.set(updated);
            return updated;
        });

    }

    private void buyerWithToken(String token) {
        when(users.findById(buyerId)).thenReturn(new User(buyerId, "b", "b@x.com", "h", "City", "Street 1", token, Instant.now()));
    }

    @Test
    void approvedPaymentMarksOrderPaid() {
        buyerWithToken("tok_ok");
        when(gateway.charge(org.mockito.ArgumentMatchers.eq("tok_ok"), org.mockito.ArgumentMatchers.eq(order.totalPaid()), org.mockito.ArgumentMatchers.startsWith("order-" + order.id())))
                .thenReturn(new PaymentResult(true, "tx1", "ok"));
        UUID artistId = UUID.randomUUID();
        when(parties.artworkOf(any())).thenReturn(java.util.Optional.of(new com.underart.domain.model.Artwork(
                UUID.randomUUID(), artistId, "Marina", null, null, null, null, null, java.util.List.of(), Instant.now())));

        assertThat(service.pay(order.id(), buyerId).paymentStatus()).isEqualTo(PaymentStatus.PAID);

        org.mockito.ArgumentCaptor<com.underart.domain.model.Notification> sent =
                org.mockito.ArgumentCaptor.forClass(com.underart.domain.model.Notification.class);
        verify(notifications, org.mockito.Mockito.times(2)).send(sent.capture());
        assertThat(sent.getAllValues()).extracting(com.underart.domain.model.Notification::userId)
                .containsExactly(buyerId, artistId);
    }

    @Test
    void declinedPaymentMarksOrderFailedAndThrows() {
        buyerWithToken("tok_fail");
        when(gateway.charge(any(), any(), any())).thenReturn(new PaymentResult(false, null, "declined"));
        assertThatThrownBy(() -> service.pay(order.id(), buyerId)).isInstanceOf(PaymentFailedException.class);
        verify(orders, org.mockito.Mockito.atLeastOnce()).save(org.mockito.ArgumentMatchers.argThat(o -> o.paymentStatus() == PaymentStatus.FAILED));
        verify(notifications).send(org.mockito.ArgumentMatchers.argThat(n ->
                n.userId().equals(buyerId) && n.type() == com.underart.domain.model.NotificationType.PAYMENT_FAILED));
    }

    @Test
    void requiresPaymentMethod() {
        buyerWithToken(null);
        assertThatThrownBy(() -> service.pay(order.id(), buyerId)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void onlyWinnerCanPay() {
        assertThatThrownBy(() -> service.pay(order.id(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenOperationException.class);
    }
    @Test
    void uncertainPaymentReusesItsReferenceAndOriginalMethod() {
        buyerWithToken("tok_original");
        when(gateway.charge(any(), any(), any()))
                .thenThrow(new PaymentFailedException("Connection interrupted"))
                .thenReturn(new PaymentResult(true, "tx_recovered", "approved"));
        assertThatThrownBy(() -> service.pay(order.id(), buyerId)).isInstanceOf(PaymentFailedException.class);
        buyerWithToken("tok_changed");
        assertThat(service.pay(order.id(), buyerId).payment().transactionId()).isEqualTo("tx_recovered");
        var references = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(gateway, org.mockito.Mockito.times(2)).charge(org.mockito.ArgumentMatchers.eq("tok_original"),
                org.mockito.ArgumentMatchers.eq(order.totalPaid()), references.capture());
        assertThat(references.getAllValues().get(0)).isEqualTo(references.getAllValues().get(1));
        assertThatThrownBy(() -> service.pay(order.id(), buyerId)).isInstanceOf(BusinessRuleException.class);
        verify(gateway, org.mockito.Mockito.times(2)).charge(any(), any(), any());
    }

}
