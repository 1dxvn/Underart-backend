package com.underart.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.underart.domain.exception.PaymentFailedException;
import com.underart.domain.port.out.PaymentGatewayPort.PaymentResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StripePaymentGatewayAdapterTest {

    private static final String BASIC = "Basic "
            + Base64.getEncoder().encodeToString("sk_test_abc:".getBytes(StandardCharsets.UTF_8));

    private MockRestServiceServer server;
    private StripePaymentGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new StripePaymentGatewayAdapter(builder.build(), "sk_test_abc", "USD");
    }

    @Test
    void registersCustomerAndAttachesPaymentMethod() {
        server.expect(requestTo("https://api.stripe.com/v1/customers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", BASIC))
                .andExpect(content().string("email=ana%40test.com"))
                .andRespond(withSuccess("{\"id\":\"cus_123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.stripe.com/v1/payment_methods/pm_card_visa/attach"))
                .andExpect(content().string("customer=cus_123"))
                .andRespond(withSuccess("{\"id\":\"pm_card_visa\"}", MediaType.APPLICATION_JSON));

        assertThat(adapter.registerPaymentMethod("ana@test.com", "pm_card_visa")).isEqualTo("cus_123:pm_card_visa");
        server.verify();
    }

    @Test
    void rejectedPaymentMethodBecomesPaymentFailed() {
        server.expect(requestTo("https://api.stripe.com/v1/customers"))
                .andRespond(withSuccess("{\"id\":\"cus_1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.stripe.com/v1/payment_methods/pm_bad/attach"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"resource_missing\",\"message\":\"No such PaymentMethod\"}}"));

        assertThatThrownBy(() -> adapter.registerPaymentMethod("ana@test.com", "pm_bad"))
                .isInstanceOf(PaymentFailedException.class).hasMessage("No such PaymentMethod");
    }

    @Test
    void chargesInCentsWithIdempotencyAndReturnsApproval() {
        server.expect(requestTo("https://api.stripe.com/v1/payment_intents"))
                .andExpect(header("Authorization", BASIC))
                .andExpect(header("Idempotency-Key", org.hamcrest.Matchers.startsWith("order-9:")))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("amount=56500"),
                        org.hamcrest.Matchers.containsString("currency=usd"),
                        org.hamcrest.Matchers.containsString("customer=cus_123"),
                        org.hamcrest.Matchers.containsString("payment_method=pm_card_visa"),
                        org.hamcrest.Matchers.containsString("confirm=true"),
                        org.hamcrest.Matchers.containsString("off_session=true"))))
                .andRespond(withSuccess("{\"id\":\"pi_1\",\"status\":\"succeeded\"}", MediaType.APPLICATION_JSON));

        PaymentResult result = adapter.charge("cus_123:pm_card_visa", new BigDecimal("565.00"), "order-9");

        assertThat(result.approved()).isTrue();
        assertThat(result.transactionId()).isEqualTo("pi_1");
        server.verify();
    }

    @Test
    void roundsAmountToTheNearestCent() {
        server.expect(requestTo("https://api.stripe.com/v1/payment_intents"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("amount=1999")))
                .andRespond(withSuccess("{\"id\":\"pi_2\",\"status\":\"succeeded\"}", MediaType.APPLICATION_JSON));

        assertThat(adapter.charge("cus_1:pm_1", new BigDecimal("19.989"), "order-1").approved()).isTrue();
    }

    @Test
    void declinedCardRaisesPaymentFailedWithStripeMessage() {
        server.expect(requestTo("https://api.stripe.com/v1/payment_intents"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"card_declined\",\"message\":\"Your card was declined.\"}}"));

        assertThatThrownBy(() -> adapter.charge("cus_1:pm_1", BigDecimal.TEN, "order-2"))
                .isInstanceOf(PaymentFailedException.class).hasMessage("Your card was declined.");
    }

    @Test
    void statusOtherThanSucceededIsNotApproved() {
        server.expect(requestTo("https://api.stripe.com/v1/payment_intents"))
                .andRespond(withSuccess("{\"id\":\"pi_3\",\"status\":\"requires_action\"}", MediaType.APPLICATION_JSON));

        PaymentResult result = adapter.charge("cus_1:pm_1", BigDecimal.TEN, "order-3");

        assertThat(result.approved()).isFalse();
        assertThat(result.message()).contains("requires_action");
    }

    @Test
    void malformedStoredTokenIsRejectedWithoutCallingStripe() {
        assertThatThrownBy(() -> adapter.charge("tok_visa", BigDecimal.TEN, "order-4"))
                .isInstanceOf(PaymentFailedException.class);
        server.verify();
    }

    @Test
    void requiresASecretKeyWhenEnabled() {
        StripePaymentGatewayAdapter withoutKey = new StripePaymentGatewayAdapter(RestClient.create(), "", "usd");
        assertThatThrownBy(withoutKey::requireSecretKey).isInstanceOf(IllegalStateException.class);
    }
}
