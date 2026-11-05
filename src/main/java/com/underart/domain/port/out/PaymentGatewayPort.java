package com.underart.domain.port.out;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    String registerPaymentMethod(String customerEmail, String paymentMethodRef);

    PaymentResult charge(String paymentToken, BigDecimal amount, String reference);

    record PaymentResult(boolean approved, String transactionId, String message) {
    }
}
