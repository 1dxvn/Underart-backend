package com.underart.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Bid(
        UUID id,
        UUID auctionId,
        UUID buyerId,
        BigDecimal amount,
        Instant bidTime) {
}
