package com.underart.domain.port.in;

import com.underart.domain.model.AuctionSummary;
import com.underart.domain.model.Bid;
import java.math.BigDecimal;
import java.util.UUID;

public interface FindAuctionUseCase {

    AuctionDetails findById(UUID id);

    record AuctionDetails(AuctionSummary summary, Bid highestBid, BigDecimal minimumNextBid) {
    }
}
