package com.underart.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ArtworkMedia(
        UUID id,
        UUID artworkId,
        MediaKind kind,
        String url,
        String storageKey,
        String contentType,
        long sizeBytes,
        int position,
        Instant createdAt) {

    public ArtworkMedia withPosition(int newPosition) {
        return new ArtworkMedia(id, artworkId, kind, url, storageKey, contentType, sizeBytes,
                newPosition, createdAt);
    }
}
