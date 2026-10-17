package com.underart.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentMethodRequest(@NotBlank @Size(max = 255) String paymentToken) {
}
