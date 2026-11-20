package com.underart.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record Auction(
        UUID id,
        UUID artworkId,
        AuctionType type,
        AuctionStatus status,
        BigDecimal basePrice,
        BigDecimal immediatePurchasePrice,
        Instant startDate,
        Instant endDate,
        AuctionCurrency currency,
        BigDecimal floorPrice) {

    public Auction(UUID id, UUID artworkId, AuctionType type, AuctionStatus status, BigDecimal basePrice,
            BigDecimal immediatePurchasePrice, Instant startDate, Instant endDate) {
        this(id, artworkId, type, status, basePrice, immediatePurchasePrice, startDate, endDate, null, null);
    }

    public boolean hasSealedBids() {
        return type == AuctionType.SEALED && isActive();
    }

    private static final BigDecimal COP_STEP = BigDecimal.valueOf(1000);

    public boolean validPriceStep(BigDecimal price) {
        return currency != AuctionCurrency.COP || price.remainder(COP_STEP).signum() == 0;
    }

    public BigDecimal minimumBid(BigDecimal highest) {
        BigDecimal minimum = type == AuctionType.SEALED ? basePrice
                : BidIncrementPolicy.minimumNextBid(basePrice, highest);
        return roundPriceUp(minimum);
    }

    private BigDecimal roundPriceUp(BigDecimal price) {
        return currency == AuctionCurrency.COP
                ? price.divide(COP_STEP, 0, RoundingMode.CEILING).multiply(COP_STEP) : price;
    }

    public BigDecimal dutchPriceAt(Instant now) {
        if (!hasStarted(now)) return basePrice;
        if (hasEnded(now)) return floorPrice;
        long total = Math.max(1, Duration.between(startDate, endDate).toMillis());
        long remaining = Duration.between(now, endDate).toMillis();
        BigDecimal price = basePrice.subtract(floorPrice).multiply(BigDecimal.valueOf(remaining))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.CEILING).add(floorPrice);
        return roundPriceUp(price).min(basePrice);
    }

    public boolean isActive() {
        return status == AuctionStatus.ACTIVE;
    }

    public boolean hasStarted(Instant now) {
        return startDate == null || !now.isBefore(startDate);
    }

    public boolean hasEnded(Instant now) {
        return !now.isBefore(endDate);
    }

    public Auction withStatus(AuctionStatus newStatus) {
        return new Auction(id, artworkId, type, newStatus, basePrice, immediatePurchasePrice,
                startDate, endDate, currency, floorPrice);
    }

    public Auction withEndDate(Instant newEndDate) {
        return new Auction(id, artworkId, type, status, basePrice, immediatePurchasePrice,
                startDate, newEndDate, currency, floorPrice);
    }
}
