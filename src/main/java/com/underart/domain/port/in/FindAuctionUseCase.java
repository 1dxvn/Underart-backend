package com.underart.domain.port.in;

import com.underart.domain.model.Auction;
import com.underart.domain.model.Bid;
import java.math.BigDecimal;
import java.util.UUID;

public interface FindAuctionUseCase {

    AuctionDetails findById(UUID id);

    record AuctionDetails(Auction auction, Bid highestBid, long bidCount, BigDecimal currentPrice) {
    }
}
