package com.underart.application.dto;

import com.underart.domain.port.in.FindAuctionUseCase.AuctionDetails;
import java.math.BigDecimal;

public record AuctionDetailResponse(
        AuctionResponse auction,
        BigDecimal currentPrice,
        long bidCount,
        BidResponse highestBid) {

    public static AuctionDetailResponse from(AuctionDetails details) {
        return new AuctionDetailResponse(AuctionResponse.from(details.auction()), details.currentPrice(),
                details.bidCount(), details.highestBid() == null ? null : BidResponse.from(details.highestBid()));
    }
}
