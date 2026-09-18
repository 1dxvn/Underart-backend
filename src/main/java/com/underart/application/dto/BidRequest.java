package com.underart.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BidRequest(
        @NotNull UUID buyerId,
        @NotNull BigDecimal amount) {
}
