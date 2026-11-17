package com.underart.application.dto;

import com.underart.domain.model.SellerStats;
import java.math.BigDecimal;

public record SellerStatsResponse(
        long artworks,
        long activeAuctions,
        long salesCount,
        BigDecimal revenue,
        long purchasesCount,
        BigDecimal averageRating,
        long reviewsCount) {

    public static SellerStatsResponse from(SellerStats s) {
        return new SellerStatsResponse(s.artworks(), s.activeAuctions(), s.salesCount(), s.revenue(),
                s.purchasesCount(), s.rating().average(), s.rating().count());
    }
}
