package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.Bid;
import com.underart.domain.port.in.PlaceBidUseCase;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;

import java.math.BigDecimal;
import java.time.Instant;

public class PlaceBidService implements PlaceBidUseCase {

    private final AuctionRepositoryPort auctions;
    private final BidRepositoryPort bids;

    public PlaceBidService(AuctionRepositoryPort auctions, BidRepositoryPort bids) {
        this.auctions = auctions;
        this.bids = bids;
    }

    @Override
    public Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount) {
        Bid bid = new Bid(null, auctionId, bidderId, amount, Instant.now());
        Auction auction = auctions.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", auctionId));
        auction.placeBid(bidderId, amount, bid.placedAt());
        auctions.save(auction);
        return bids.save(bid);
    }
}
