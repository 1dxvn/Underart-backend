package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import java.util.UUID;

public interface AcceptAuctionBidUseCase {
    Order acceptHighest(UUID auctionId, UUID sellerId, UUID expectedBidId);
}
