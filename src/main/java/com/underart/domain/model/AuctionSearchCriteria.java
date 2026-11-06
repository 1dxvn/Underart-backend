package com.underart.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuctionSearchCriteria(
        AuctionStatus status,
        AuctionType type,
        UUID artworkId,
        UUID artistId,
        Instant endingBefore,
        PriceRange basePrice,
        AuctionSortField sortBy,
        boolean ascending) {

    public AuctionSearchCriteria {
        basePrice = basePrice == null ? PriceRange.ANY : basePrice;
        sortBy = sortBy == null ? AuctionSortField.END_DATE : sortBy;
    }

    public static AuctionSearchCriteria byStatus(AuctionStatus status) {
        return new AuctionSearchCriteria(status, null, null, null, null, PriceRange.ANY, AuctionSortField.END_DATE, true);
    }

    public static AuctionSearchCriteria byArtist(UUID artistId) {
        return new AuctionSearchCriteria(null, null, null, artistId, null, PriceRange.ANY, AuctionSortField.END_DATE, true);
    }
}
