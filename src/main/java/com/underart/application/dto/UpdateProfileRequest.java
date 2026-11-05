package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 60) String username,
        @Size(max = 255) String city,
        @Size(max = 255) String shippingAddress) {
}
