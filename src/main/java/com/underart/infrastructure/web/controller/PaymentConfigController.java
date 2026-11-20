package com.underart.infrastructure.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentConfigController {

    private final PaymentConfiguration configuration;

    public PaymentConfigController(@Value("${payment.provider:simulated}") String provider,
            @Value("${payment.stripe.publishable-key:}") String publishableKey,
            @Value("${MERCADO_PAGO_TEST_PUBLIC_KEY:}") String mercadoPagoKey) {
        String key = switch (provider) {
            case "stripe" -> publishableKey;
            case "mercadopago" -> mercadoPagoKey;
            default -> "";
        };
        configuration = new PaymentConfiguration(provider, key);
    }

    @GetMapping("/api/v1/payments/config")
    public PaymentConfiguration config() {
        return configuration;
    }

    public record PaymentConfiguration(String provider, String publishableKey) {}
}
