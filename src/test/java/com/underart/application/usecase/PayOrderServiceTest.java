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
    private final PayOrderService service = new PayOrderService(orders, users, gateway, log);
    private final UUID buyerId = UUID.randomUUID();
    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.pending(UUID.randomUUID(), buyerId, new BigDecimal("100.00"));
        when(orders.findById(order.id())).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void buyerWithToken(String token) {
        when(users.findById(buyerId)).thenReturn(new User(buyerId, "b", "b@x.com", "h", null, null, token, Instant.now()));
    }

    @Test
    void approvedPaymentMarksOrderPaid() {
        buyerWithToken("tok_ok");
        when(gateway.charge("tok_ok", order.totalPaid(), "order-" + order.id()))
                .thenReturn(new PaymentResult(true, "tx1", "ok"));
        assertThat(service.pay(order.id(), buyerId).paymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void declinedPaymentMarksOrderFailedAndThrows() {
        buyerWithToken("tok_fail");
        when(gateway.charge(any(), any(), any())).thenReturn(new PaymentResult(false, null, "rechazada"));
        assertThatThrownBy(() -> service.pay(order.id(), buyerId)).isInstanceOf(PaymentFailedException.class);
        verify(orders).save(order.withPaymentStatus(PaymentStatus.FAILED));
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
}
