package com.underart.application.dto;

import com.underart.domain.model.AuctionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAuctionRequest(
    @NotNull UUID artworkId,
    @NotNull AuctionType type,
    @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal basePrice,
    @Positive @Digits(integer = 12, fraction = 2) BigDecimal immediatePurchasePrice,
    Instant startDate,
    @NotNull @Future Instant endDate
) {}
