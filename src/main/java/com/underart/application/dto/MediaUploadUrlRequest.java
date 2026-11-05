package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record MediaUploadUrlRequest(@NotBlank String contentType, @Positive long sizeBytes) {
}
