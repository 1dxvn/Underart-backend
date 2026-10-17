package com.underart.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuctionSearchCriteria(
        AuctionStatus status,
        AuctionType type,
        UUID artworkId,
        UUID artistId,
        Instant endingBefore) {

    public static AuctionSearchCriteria byStatus(AuctionStatus status) {
        return new AuctionSearchCriteria(status, null, null, null, null);
    }

    public static AuctionSearchCriteria byArtist(UUID artistId) {
        return new AuctionSearchCriteria(null, null, null, artistId, null);
    }
}
