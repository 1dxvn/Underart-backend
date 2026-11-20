package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import java.util.UUID;
import com.underart.domain.port.out.PaymentGatewayPort.CheckoutCard;

public interface PayOrderUseCase {

    Order pay(UUID orderId, UUID requesterId);

    Order pay(UUID orderId, UUID requesterId, CheckoutCard card);
}
