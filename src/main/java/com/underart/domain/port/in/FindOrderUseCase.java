package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import java.util.UUID;

public interface FindOrderUseCase {

    Order findById(UUID orderId, UUID requesterId);
}
