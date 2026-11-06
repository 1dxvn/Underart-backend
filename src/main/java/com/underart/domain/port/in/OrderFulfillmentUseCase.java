package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import java.util.UUID;

public interface OrderFulfillmentUseCase {

    Order ship(UUID orderId, UUID sellerId, String carrier, String trackingNumber);
}
