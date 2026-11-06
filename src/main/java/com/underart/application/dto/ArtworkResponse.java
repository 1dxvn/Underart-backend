package com.underart.application.dto;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArtworkResponse(
        UUID id,
        UUID artistId,
        String title,
        String description,
        String technique,
        BigDecimal weightKg,
        String dimensions,
        BigDecimal suggestedPrice,
        List<String> tags,
        String coverUrl,
        Instant createdAt) {

    public static ArtworkResponse from(Artwork a) {
        return from(a, null);
    }

    public static ArtworkResponse from(ArtworkSummary summary) {
        return from(summary.artwork(), summary.coverUrl());
    }

    public static ArtworkResponse from(Artwork a, String coverUrl) {
        return new ArtworkResponse(a.id(), a.artistId(), a.title(), a.description(), a.technique(),
                a.weightKg(), a.dimensions(), a.suggestedPrice(), a.tags(), coverUrl, a.createdAt());
    }
}
