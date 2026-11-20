package com.underart.domain.port.out;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    String registerPaymentMethod(String customerEmail, String paymentMethodRef);

    PaymentResult charge(String paymentToken, BigDecimal amount, String reference);

    default PaymentResult charge(String paymentToken, BigDecimal amount, String reference, String currency) {
        return charge(paymentToken, amount, reference);
    }

    default String prepareCheckout(CheckoutCard card, String currency) {
        if (card != null) {
            throw new com.underart.domain.exception.BusinessRuleException("La pasarela no admite este formulario.");
        }
        return null;
    }

    record CheckoutCard(String token, String method, String issuer, int installments) {}

    record PaymentResult(boolean approved, String transactionId, String message) {
    }
}
