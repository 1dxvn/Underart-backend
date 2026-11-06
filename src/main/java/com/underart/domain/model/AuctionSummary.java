package com.underart.domain.model;

import java.math.BigDecimal;

public record AuctionSummary(
        Auction auction,
        String artworkTitle,
        String coverUrl,
        BigDecimal currentPrice,
        long bidCount) {
}
