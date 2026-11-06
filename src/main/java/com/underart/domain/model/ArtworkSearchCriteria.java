package com.underart.domain.model;

import java.util.UUID;

public record ArtworkSearchCriteria(
        UUID artistId,
        String technique,
        String tag,
        String text,
        PriceRange price,
        ArtworkSortField sortBy,
        boolean ascending) {

    public ArtworkSearchCriteria {
        price = price == null ? PriceRange.ANY : price;
        sortBy = sortBy == null ? ArtworkSortField.CREATED_AT : sortBy;
    }

    public static ArtworkSearchCriteria byArtist(UUID artistId) {
        return new ArtworkSearchCriteria(artistId, null, null, null, PriceRange.ANY, ArtworkSortField.CREATED_AT, false);
    }
}
