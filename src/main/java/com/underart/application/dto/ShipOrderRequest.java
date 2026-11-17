package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipOrderRequest(
        @NotBlank @Size(max = 100) String carrier,
        @NotBlank @Size(max = 100) String trackingNumber) {
}
