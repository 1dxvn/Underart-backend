package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiValuationRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @Size(max = 255) String technique,
        @Size(max = 255) String dimensions) {
}
