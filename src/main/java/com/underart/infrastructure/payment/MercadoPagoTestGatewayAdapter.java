package com.underart.infrastructure.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.PaymentPendingException;
import com.underart.domain.port.out.PaymentGatewayPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mercadopago")
public class MercadoPagoTestGatewayAdapter implements PaymentGatewayPort {

    private static final String PREFIX = "mp-test:";
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String buyerEmail;

    @Autowired
    public MercadoPagoTestGatewayAdapter(
            @Value("${MERCADO_PAGO_TEST_ACCESS_TOKEN:}") String token,
            @Value("${MERCADO_PAGO_TEST_PUBLIC_KEY:}") String publicKey,
            @Value("${MERCADO_PAGO_TEST_BUYER_EMAIL:}") String buyerEmail, ObjectMapper mapper) {
        this(buildClient(token), buyerEmail, mapper);
        if (publicKey.isBlank()) throw new IllegalStateException("Configure MERCADO_PAGO_TEST_PUBLIC_KEY");
    }

    MercadoPagoTestGatewayAdapter(RestClient client, String buyerEmail, ObjectMapper mapper) {
        if (buyerEmail.isBlank()) throw new IllegalStateException("Configure MERCADO_PAGO_TEST_BUYER_EMAIL");
        this.client = client;
        this.buyerEmail = buyerEmail;
        this.mapper = mapper;
    }

    @Override
    public String registerPaymentMethod(String customerEmail, String method) {
        throw new BusinessRuleException("Mercado Pago usa una tarjeta de prueba para cada pedido.");
    }

    @Override
    public String prepareCheckout(CheckoutCard card, String currency) {
        if (!"cop".equalsIgnoreCase(currency)) {
            throw new BusinessRuleException("La prueba de Mercado Pago está disponible para órdenes en COP.");
        }
        if (card == null) throw new BusinessRuleException("Completa el formulario de prueba de Mercado Pago.");
        try {
            return PREFIX + mapper.writeValueAsString(card);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid checkout card", ex);
        }
    }

    @Override
    public PaymentResult charge(String token, BigDecimal amount, String reference) {
        return charge(token, amount, reference, "cop");
    }

    @Override
    public PaymentResult charge(String token, BigDecimal amount, String reference, String currency) {
        if (!"cop".equalsIgnoreCase(currency) || token == null || !token.startsWith(PREFIX)) {
            return new PaymentResult(false, null, "Completa un nuevo intento de prueba en pesos colombianos.");
        }
        try {
            CheckoutCard card = mapper.readValue(token.substring(PREFIX.length()), CheckoutCard.class);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("token", card.token());
            payload.put("transaction_amount", amount);
            payload.put("external_reference", reference);
            payload.put("payment_method_id", card.method());
            payload.put("installments", card.installments());
            if (card.issuer() != null && !card.issuer().isBlank()) payload.put("issuer_id", card.issuer());
            payload.put("payer", Map.of("email", buyerEmail,
                    "identification", Map.of("type", "CC", "number", "123456789")));

            String attemptKey = UUID.nameUUIDFromBytes(reference.getBytes(StandardCharsets.UTF_8)).toString();
            JsonNode payment = client.post().uri("/v1/payments").header("X-Idempotency-Key", attemptKey)
                    .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(JsonNode.class);
            validateTestPayment(payment, reference, amount);
            if (isPending(payment)) {
                payment = client.get().uri("/v1/payments/{id}", payment.path("id").asText())
                        .retrieve().body(JsonNode.class);
                validateTestPayment(payment, reference, amount);
            }
            return switch (payment.path("status").asText()) {
                case "approved" -> new PaymentResult(true, payment.path("id").asText(), "Pago de prueba aprobado");
                case "rejected", "cancelled" -> new PaymentResult(false, payment.path("id").asText(),
                        "Mercado Pago rechazó el pago de prueba.");
                default -> throw pending();
            };
        } catch (RestClientException | JsonProcessingException ex) {
            throw pending();
        }
    }

    private void validateTestPayment(JsonNode payment, String reference, BigDecimal amount) {
        if (payment == null || !payment.path("live_mode").isBoolean() || payment.path("live_mode").asBoolean()
                || payment.path("id").asText().isBlank()
                || !reference.equals(payment.path("external_reference").asText())
                || !"COP".equals(payment.path("currency_id").asText())
                || !payment.path("transaction_amount").isNumber()
                || amount.compareTo(payment.path("transaction_amount").decimalValue()) != 0) {
            throw pending();
        }
    }

    private boolean isPending(JsonNode payment) {
        return switch (payment.path("status").asText()) {
            case "approved", "rejected", "cancelled" -> false;
            default -> true;
        };
    }

    private PaymentPendingException pending() {
        return new PaymentPendingException("Mercado Pago todavía no confirma el resultado. Consulta el mismo intento desde la orden.");
    }

    private static RestClient buildClient(String token) {
        if (token.isBlank()) throw new IllegalStateException("Configure MERCADO_PAGO_TEST_ACCESS_TOKEN");
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return RestClient.builder().baseUrl("https://api.mercadopago.com")
                .defaultHeaders(headers -> headers.setBearerAuth(token)).requestFactory(factory).build();
    }
}
