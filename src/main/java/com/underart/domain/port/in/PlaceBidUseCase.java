package com.underart.domain.port.in;

import com.underart.domain.model.Bid;
import java.math.BigDecimal;
import java.util.UUID;

public interface PlaceBidUseCase {

    Bid place(UUID auctionId, UUID buyerId, BigDecimal amount);
}
