package com.underart.application.dto;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuctionResponse(
        UUID id,
        UUID artworkId,
        AuctionType type,
        AuctionStatus status,
        BigDecimal basePrice,
        BigDecimal immediatePurchasePrice,
        Instant startDate,
        Instant endDate) {

    public static AuctionResponse from(Auction a) {
        return new AuctionResponse(a.id(), a.artworkId(), a.type(), a.status(), a.basePrice(),
                a.immediatePurchasePrice(), a.startDate(), a.endDate());
    }
}
