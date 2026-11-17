package com.underart.application.dto;

import com.underart.domain.port.in.FindAuctionUseCase.AuctionDetails;
import java.math.BigDecimal;

public record AuctionDetailResponse(
        AuctionResponse auction,
        BidResponse highestBid,
        BigDecimal minimumNextBid) {

    public static AuctionDetailResponse from(AuctionDetails details) {
        return new AuctionDetailResponse(AuctionResponse.from(details.summary()),
                details.highestBid() == null ? null : BidResponse.from(details.highestBid()),
                details.minimumNextBid());
    }
}
