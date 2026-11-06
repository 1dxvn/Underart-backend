package com.underart.application.mapper;

import com.underart.application.dto.AuctionResponse;
import com.underart.application.dto.BidResponse;
import com.underart.application.dto.OrderResponse;
import com.underart.domain.model.AuctionSummary;
import com.underart.domain.model.Bid;
import com.underart.domain.model.Order;

public final class AuctionDtoMapper {

    private AuctionDtoMapper() {
    }

    public static AuctionResponse toResponse(AuctionSummary summary) {
        return AuctionResponse.from(summary);
    }

    public static BidResponse toResponse(Bid bid) {
        return BidResponse.from(bid);
    }

    public static OrderResponse toResponse(Order order) {
        return OrderResponse.from(order);
    }
}
