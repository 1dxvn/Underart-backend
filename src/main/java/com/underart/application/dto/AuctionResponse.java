package com.underart.application.dto;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionCurrency;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionSummary;
import com.underart.domain.model.AuctionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuctionResponse(
        UUID id,
        UUID artworkId,
        String artworkTitle,
        String coverUrl,
        AuctionType type,
        AuctionStatus status,
        BigDecimal basePrice,
        BigDecimal immediatePurchasePrice,
        BigDecimal currentPrice,
        long bidCount,
        Instant startDate,
        Instant endDate,
        AuctionCurrency currency,
        BigDecimal floorPrice) {

    public static AuctionResponse from(AuctionSummary s) {
        Auction a = s.auction();
        return new AuctionResponse(a.id(), a.artworkId(), s.artworkTitle(), s.coverUrl(), a.type(), a.status(),
                a.basePrice(), a.immediatePurchasePrice(), s.currentPrice(), s.bidCount(), a.startDate(),
                a.endDate(), a.currency(), a.floorPrice());
    }
}
