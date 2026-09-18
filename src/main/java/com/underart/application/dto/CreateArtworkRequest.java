package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateArtworkRequest(
        @NotNull UUID artistId,
        @NotBlank String title,
        String description,
        String technique,
        BigDecimal weightKg,
        String dimensions) {
}
