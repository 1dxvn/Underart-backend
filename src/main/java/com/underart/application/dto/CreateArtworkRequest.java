package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateArtworkRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @Size(max = 255) String technique,
        @Positive BigDecimal weightKg,
        @Size(max = 255) String dimensions) {
}
