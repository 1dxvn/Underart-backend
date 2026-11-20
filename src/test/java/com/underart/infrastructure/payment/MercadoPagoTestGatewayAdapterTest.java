package com.underart.infrastructure.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.PaymentPendingException;
import com.underart.domain.port.out.PaymentGatewayPort.CheckoutCard;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoTestGatewayAdapterTest {

    private static final BigDecimal TOTAL = new BigDecimal("40000.00");
    private final RestClient.Builder client = RestClient.builder().baseUrl("https://api.mercadopago.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
    private final MercadoPagoTestGatewayAdapter gateway =
            new MercadoPagoTestGatewayAdapter(client.build(), "buyer@example.test", new ObjectMapper());
    private final CheckoutCard card = new CheckoutCard("card-token", "visa", "1", 1);

    @Test
    void confirmsTestPaymentWithServerAmountAndReusesTheAttemptKey() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "1db222fe-ce0e-3454-8122-788bf892adf8"))
                .andExpect(jsonPath("$.transaction_amount").value(40000.0))
                .andExpect(jsonPath("$.external_reference").value("attempt"))
                .andExpect(jsonPath("$.token").value("card-token"))
                .andExpect(jsonPath("$.payer.email").value("buyer@example.test"))
                .andRespond(withSuccess(payment(false, "in_process"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(payment(false, "approved"), MediaType.APPLICATION_JSON));

        var result = gateway.charge(gateway.prepareCheckout(card, "cop"), TOTAL, "attempt", "cop");

        assertThat(result.approved()).isTrue();
        server.verify();
    }

    @Test
    void neverSettlesAnOrderUsingAProductionResponse() {
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andRespond(withSuccess(payment(true, "approved"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.charge(gateway.prepareCheckout(card, "cop"), TOTAL, "attempt", "cop"))
                .isInstanceOf(PaymentPendingException.class);
        server.verify();
    }

    @Test
    void rejectsCurrenciesOutsideTheColombianSandbox() {
        assertThatThrownBy(() -> gateway.prepareCheckout(card, "eur")).isInstanceOf(BusinessRuleException.class);
    }

    private String payment(boolean live, String status) {
        return """
                {"id":123,"external_reference":"attempt","currency_id":"COP",
                 "transaction_amount":40000,"live_mode":%s,"status":"%s"}
                """.formatted(live, status);
    }
}
