package com.underart.application.dto;

import com.underart.domain.model.Artwork;
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
        Instant createdAt) {

    public static ArtworkResponse from(Artwork a) {
        return new ArtworkResponse(a.id(), a.artistId(), a.title(), a.description(), a.technique(),
                a.weightKg(), a.dimensions(), a.suggestedPrice(), a.tags(), a.createdAt());
    }
}
