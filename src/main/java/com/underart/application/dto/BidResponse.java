package com.underart.application.dto;

import com.underart.domain.model.Bid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidResponse(
        UUID id,
        UUID auctionId,
        UUID buyerId,
        BigDecimal amount,
        Instant bidTime) {

    public static BidResponse from(Bid b) {
        return new BidResponse(b.id(), b.auctionId(), b.buyerId(), b.amount(), b.bidTime());
    }
}
