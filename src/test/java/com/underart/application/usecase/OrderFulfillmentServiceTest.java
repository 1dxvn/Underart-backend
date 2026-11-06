package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.model.ShippingStatus;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderFulfillmentServiceTest {

    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final OrderParties parties = mock(OrderParties.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final NotificationSenderPort notifications = mock(NotificationSenderPort.class);
    private final OrderFulfillmentService service = new OrderFulfillmentService(orders, parties, log, notifications);
    private final UUID sellerId = UUID.randomUUID();
    private final UUID buyerId = UUID.randomUUID();
    private Order paid;

    @BeforeEach
    void setUp() {
        paid = Order.pending(UUID.randomUUID(), buyerId, new BigDecimal("100.00")).withPaymentStatus(PaymentStatus.PAID);
        when(orders.findById(paid.id())).thenReturn(Optional.of(paid));
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(parties.isSeller(paid, sellerId)).thenReturn(true);
    }

    @Test
    void sellerShipsAPaidOrder() {
        Order shipped = service.ship(paid.id(), sellerId, " DHL ", " 123 ");

        assertThat(shipped.shippingStatus()).isEqualTo(ShippingStatus.SHIPPED);
        assertThat(shipped.carrier()).isEqualTo("DHL");
        assertThat(shipped.trackingNumber()).isEqualTo("123");
        verify(notifications).send(org.mockito.ArgumentMatchers.argThat(n -> n.userId().equals(buyerId)
                && n.type() == com.underart.domain.model.NotificationType.ORDER_SHIPPED && n.message().contains("123")));
    }

    @Test
    void onlyTheSellerCanShip() {
        assertThatThrownBy(() -> service.ship(paid.id(), buyerId, "DHL", "123"))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void buyerConfirmsDeliveryOfAShippedOrder() {
        Order shipped = paid.markShipped("DHL", "123", java.time.Instant.now());
        when(orders.findById(shipped.id())).thenReturn(Optional.of(shipped));

        assertThat(service.confirmDelivery(shipped.id(), buyerId).shippingStatus()).isEqualTo(ShippingStatus.DELIVERED);
        assertThatThrownBy(() -> service.confirmDelivery(shipped.id(), sellerId))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
