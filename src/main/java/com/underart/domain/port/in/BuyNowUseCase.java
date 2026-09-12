package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import java.util.UUID;

public interface BuyNowUseCase {

    Order buy(UUID auctionId, UUID buyerId);
}
