package com.underart.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100, message = "debe tener entre 8 y 100 caracteres") String password,
        @Size(max = 255) String city,
        @Size(max = 255) String shippingAddress) {
}
