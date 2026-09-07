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
}
