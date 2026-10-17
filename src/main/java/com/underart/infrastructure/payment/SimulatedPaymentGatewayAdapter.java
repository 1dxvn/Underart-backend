package com.underart.infrastructure.payment;

import com.underart.domain.port.out.PaymentGatewayPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String DECLINED_PREFIX = "tok_fail";

    @Override
    public PaymentResult charge(String paymentToken, BigDecimal amount, String reference) {
        if (paymentToken.startsWith(DECLINED_PREFIX)) {
            return new PaymentResult(false, null, "tarjeta rechazada (simulación)");
        }
        return new PaymentResult(true, "sim_" + UUID.randomUUID(), "aprobado (simulación)");
    }
}
