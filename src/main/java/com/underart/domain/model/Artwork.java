package com.underart.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Artwork(
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
}
