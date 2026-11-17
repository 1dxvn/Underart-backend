package com.underart.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.underart.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private final Order pending = Order.pending(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("500.00"));

    @Test
    void pendingOrderAddsShippingAndFee() {
        assertThat(pending.totalPaid()).isEqualByComparingTo("565.00");
        assertThat(pending.shippingStatus()).isEqualTo(ShippingStatus.PENDING);
        assertThat(pending.trackingNumber()).isNull();
    }

    @Test
    void onlyPaidOrdersCanBeShippedAndOnlyOnce() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> pending.markShipped("DHL", "123", now))
                .isInstanceOf(BusinessRuleException.class).hasMessage("The order has not been paid");

        Order shipped = pending.withPaymentStatus(PaymentStatus.PAID).markShipped("DHL", "123", now);

        assertThat(shipped.shippingStatus()).isEqualTo(ShippingStatus.SHIPPED);
        assertThat(shipped.carrier()).isEqualTo("DHL");
        assertThat(shipped.trackingNumber()).isEqualTo("123");
        assertThat(shipped.shippedAt()).isEqualTo(now);
        assertThatThrownBy(() -> shipped.markShipped("DHL", "456", now))
                .isInstanceOf(BusinessRuleException.class).hasMessage("The order has already been shipped");
    }

    @Test
    void onlyShippedOrdersCanBeDelivered() {
        assertThatThrownBy(() -> pending.markDelivered(Instant.now()))
                .isInstanceOf(BusinessRuleException.class).hasMessage("The order has not been shipped");

        Order delivered = pending.withPaymentStatus(PaymentStatus.PAID).markShipped("DHL", "1", Instant.now())
                .markDelivered(Instant.now());

        assertThat(delivered.shippingStatus()).isEqualTo(ShippingStatus.DELIVERED);
        assertThat(delivered.deliveredAt()).isNotNull();
        assertThatThrownBy(() -> delivered.markDelivered(Instant.now()))
                .isInstanceOf(BusinessRuleException.class).hasMessage("The order has already been delivered");
    }
    @org.junit.jupiter.api.Test
    void refusesUnsafeRetriesAndKeepsProcessingOrdersReserved() {
        java.time.Instant started = java.time.Instant.now().minus(java.time.Duration.ofHours(24));
        var buyer = new User(java.util.UUID.randomUUID(), "Buyer", "buyer@test.com", "hash", "City", "Street",
                "tok_visa", started);
        Order pending = Order.pending(java.util.UUID.randomUUID(), buyer.id(), java.math.BigDecimal.TEN);
        Order processing = pending.beginPayment(buyer, started);
        assertThatThrownBy(() -> processing.beginPayment(buyer, java.time.Instant.now()))
                .isInstanceOf(com.underart.domain.exception.BusinessRuleException.class)
                .hasMessageContaining("manual reconciliation");
    }

}
