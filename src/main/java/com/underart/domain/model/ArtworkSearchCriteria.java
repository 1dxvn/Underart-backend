package com.underart.domain.model;

import java.util.UUID;

public record ArtworkSearchCriteria(UUID artistId, String technique, String tag, String text) {

    public static ArtworkSearchCriteria byArtist(UUID artistId) {
        return new ArtworkSearchCriteria(artistId, null, null, null);
    }
}
