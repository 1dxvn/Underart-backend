package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import java.util.UUID;

public interface PayOrderUseCase {

    Order pay(UUID orderId, UUID requesterId);
}
