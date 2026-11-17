package com.underart.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.underart.domain.exception.PaymentFailedException;
import com.underart.domain.port.out.PaymentGatewayPort;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String BASE_URL = "https://api.stripe.com/v1";
    private static final String TOKEN_SEPARATOR = ":";

    private final RestClient client;
    private final String secretKey;
    private final String currency;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public StripePaymentGatewayAdapter(@Value("${payment.stripe.secret-key:}") String secretKey,
                                       @Value("${payment.currency:usd}") String currency) {
        this(RestClient.builder().requestFactory(timeouts()).build(), secretKey, currency);
    }

    StripePaymentGatewayAdapter(RestClient client, String secretKey, String currency) {
        this.client = client;
        this.secretKey = secretKey;
        this.currency = currency.toLowerCase();
    }

    @PostConstruct
    void requireSecretKey() {
        if (secretKey == null || !secretKey.startsWith("sk_")) {
            throw new IllegalStateException("payment.provider=stripe requires STRIPE_SECRET_KEY (sk_test_... or sk_live_...)");
        }
    }

    @Override
    public String registerPaymentMethod(String customerEmail, String paymentMethodRef) {
        MultiValueMap<String, String> customer = new LinkedMultiValueMap<>();
        customer.add("email", customerEmail);
        String customerId = require(post("/customers", customer, null), "id");
        MultiValueMap<String, String> attach = new LinkedMultiValueMap<>();
        attach.add("customer", customerId);
        post("/payment_methods/" + paymentMethodRef + "/attach", attach, null);
        return customerId + TOKEN_SEPARATOR + paymentMethodRef;
    }

    @Override
    public PaymentResult charge(String paymentToken, BigDecimal amount, String reference) {
        String[] parts = paymentToken.split(TOKEN_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new PaymentFailedException("Invalid stored payment method. Register it again");
        }
        MultiValueMap<String, String> intent = new LinkedMultiValueMap<>();
        intent.add("amount", String.valueOf(amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()));
        intent.add("currency", currency);
        intent.add("customer", parts[0]);
        intent.add("payment_method", parts[1]);
        intent.add("payment_method_types[]", "card");
        intent.add("confirm", "true");
        intent.add("off_session", "true");
        intent.add("description", reference);
        intent.add("metadata[reference]", reference);
        JsonNode response = post("/payment_intents", intent, reference);
        String status = response.path("status").asText();
        if ("succeeded".equals(status)) {
            return new PaymentResult(true, require(response, "id"), "approved");
        }
        if ("requires_payment_method".equals(status) || "canceled".equals(status)) {
            return new PaymentResult(false, response.path("id").asText(null), "Payment declined");
        }
        throw new PaymentFailedException("Payment is unresolved (" + status + "). Retry the same order payment");
    }

    private JsonNode post(String path, MultiValueMap<String, String> form, String idempotencyKey) {
        try {
            return client.post().uri(BASE_URL + path)
                    .headers(h -> {
                        h.setBasicAuth(secretKey, "");
                        if (idempotencyKey != null) {
                            h.set("Idempotency-Key", idempotencyKey);
                        }
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .exchange((request, response) -> {
                        JsonNode body = mapper.readTree(response.getBody());
                        if (response.getStatusCode().isError()) {
                            String message = body.path("error").path("message").asText("unknown error");
                            log.warn("Stripe rejected {} ({}): {}", path, response.getStatusCode().value(),
                                    body.path("error").path("code").asText());
                            if (path.equals("/payment_intents") && response.getStatusCode().value() == 402
                                    && body.path("error").path("code").asText().equals("card_declined")) {
                                return mapper.createObjectNode().put("status", "requires_payment_method");
                            }
                            throw new PaymentFailedException(message);
                        }
                        return body;
                    });
        } catch (PaymentFailedException e) {
            throw e;
        } catch (RestClientException | IllegalStateException e) {
            log.error("Could not contact Stripe at {}", path, e);
            throw new PaymentFailedException("Could not contact the payment gateway");
        }
    }

    private static String require(JsonNode body, String field) {
        String value = body.path(field).asText("");
        if (value.isBlank()) {
            throw new PaymentFailedException("Unexpected payment gateway response");
        }
        return value;
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        return factory;
    }
}
