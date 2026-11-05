package com.underart.application.dto;

import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.model.MediaKind;
import java.time.Instant;
import java.util.UUID;

public record ArtworkMediaResponse(
        UUID id,
        UUID artworkId,
        MediaKind kind,
        String url,
        String contentType,
        long sizeBytes,
        int position,
        boolean cover,
        Instant createdAt) {

    public static ArtworkMediaResponse from(ArtworkMedia m) {
        return new ArtworkMediaResponse(m.id(), m.artworkId(), m.kind(), m.url(), m.contentType(),
                m.sizeBytes(), m.position(), m.position() == 0, m.createdAt());
    }
}
