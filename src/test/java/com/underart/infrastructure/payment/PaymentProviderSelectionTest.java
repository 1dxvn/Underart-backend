package com.underart.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PaymentProviderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SimulatedPaymentGatewayAdapter.class, StripePaymentGatewayAdapter.class);

    @Test
    void usesTheSimulatedGatewayByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(SimulatedPaymentGatewayAdapter.class);
            assertThat(ctx).doesNotHaveBean(StripePaymentGatewayAdapter.class);
        });
    }

    @Test
    void usesStripeWhenConfigured() {
        runner.withPropertyValues("payment.provider=stripe", "payment.stripe.secret-key=sk_test_abc")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(StripePaymentGatewayAdapter.class);
                    assertThat(ctx).doesNotHaveBean(SimulatedPaymentGatewayAdapter.class);
                });
    }

    @Test
    void stripeWithoutSecretKeyFailsToStart() {
        runner.withPropertyValues("payment.provider=stripe")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
