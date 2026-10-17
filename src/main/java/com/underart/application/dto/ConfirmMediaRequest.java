package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmMediaRequest(@NotBlank String contentType) {
}
