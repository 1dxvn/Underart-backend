package com.underart.application.dto;

import com.underart.domain.model.AuctionType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAuctionRequest(
    @NotNull UUID artworkId,
    @NotNull AuctionType type,
    @NotNull BigDecimal basePrice,
    BigDecimal immediatePurchasePrice,
    Instant startDate,
    @NotNull Instant endDate
) {}
