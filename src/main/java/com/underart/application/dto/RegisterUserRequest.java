package com.underart.application.dto;

import jakarta.validation.constraints.Email;
import com.underart.application.dto.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @StrongPassword String password,
        @Size(max = 255) String city,
        @Size(max = 255) String shippingAddress) {
}
