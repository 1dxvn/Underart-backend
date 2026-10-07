package com.underart.domain.port.in;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface CreateAuctionUseCase {

    Auction create(CreateAuctionCommand cmd);

    record CreateAuctionCommand(
            UUID artworkId,
            AuctionType type,
            BigDecimal basePrice,
            BigDecimal immediatePurchasePrice,
            Instant startDate,
            Instant endDate) {
    }
}
