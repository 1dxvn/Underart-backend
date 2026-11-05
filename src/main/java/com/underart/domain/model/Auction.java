package com.underart.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Auction(
        UUID id,
        UUID artworkId,
        AuctionType type,
        AuctionStatus status,
        BigDecimal basePrice,
        BigDecimal immediatePurchasePrice,
        Instant startDate,
        Instant endDate) {

    public boolean isActive() {
        return status == AuctionStatus.ACTIVE;
    }

    public boolean hasStarted(Instant now) {
        return startDate == null || !now.isBefore(startDate);
    }

    public boolean hasEnded(Instant now) {
        return now.isAfter(endDate);
    }

    public Auction withStatus(AuctionStatus newStatus) {
        return new Auction(id, artworkId, type, newStatus, basePrice, immediatePurchasePrice,
                startDate, endDate);
    }
}
