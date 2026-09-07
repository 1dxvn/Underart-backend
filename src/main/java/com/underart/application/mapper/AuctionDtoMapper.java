package com.underart.application.mapper;

import com.underart.application.dto.AuctionResponse;
import com.underart.application.dto.BidResponse;
import com.underart.application.dto.OrderResponse;
import com.underart.domain.model.Auction;
import com.underart.domain.model.Bid;
import com.underart.domain.model.Order;

public final class AuctionDtoMapper {

    private AuctionDtoMapper() {}

    public static AuctionResponse toResponse(Auction a) {
        return new AuctionResponse(a.getId(), a.getArtworkId(), a.getSellerId(), a.getType(), a.getStatus(),
                a.getCurrentPrice(), a.getBuyNowPrice(), a.getHighestBidderId(), a.getEndsAt());
    }

    public static BidResponse toResponse(Bid b) {
        return new BidResponse(b.id(), b.auctionId(), b.bidderId(), b.amount(), b.placedAt());
    }

    public static OrderResponse toResponse(Order o) {
        return new OrderResponse(o.id(), o.auctionId(), o.buyerId(), o.amount(), o.status(), o.createdAt());
    }
}
