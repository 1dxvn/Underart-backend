package com.underart.domain.model;

import java.math.BigDecimal;

public record SellerStats(
        long artworks,
        long activeAuctions,
        long salesCount,
        BigDecimal revenue,
        long purchasesCount,
        RatingSummary rating) {
}
