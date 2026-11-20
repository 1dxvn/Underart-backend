package com.underart.domain.model.builder;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionCurrency;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AuctionBuilder {

    private final UUID id = UUID.randomUUID();
    private final AuctionStatus status = AuctionStatus.ACTIVE;
    private UUID artworkId;
    private AuctionType type = AuctionType.SIMPLE;
    private BigDecimal basePrice;
    private BigDecimal immediatePurchasePrice;
    private AuctionCurrency currency;
    private BigDecimal floorPrice;
    private Instant startDate;
    private Instant endDate;

    public AuctionBuilder withArtwork(UUID artworkId) {
        this.artworkId = artworkId;
        return this;
    }

    public AuctionBuilder withType(AuctionType type) {
        this.type = type;
        return this;
    }

    public AuctionBuilder withBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
        return this;
    }

    public AuctionBuilder withImmediatePurchasePrice(BigDecimal price) {
        this.immediatePurchasePrice = price;
        return this;
    }

    public AuctionBuilder withStartDate(Instant start) {
        this.startDate = start;
        return this;
    }

    public AuctionBuilder withEndDate(Instant end) {
        this.endDate = end;
        return this;
    }

    public AuctionBuilder withCurrency(AuctionCurrency value) {
        this.currency = value;
        return this;
    }

    public AuctionBuilder withFloorPrice(BigDecimal value) {
        this.floorPrice = value;
        return this;
    }

    public Auction build() {
        if (artworkId == null || basePrice == null || endDate == null) {
            throw new IllegalStateException("Missing artwork, basePrice or endDate");
        }
        Instant start = startDate != null ? startDate : Instant.now();
        if (basePrice.signum() <= 0 || basePrice.scale() > 2 || type == null || !endDate.isAfter(start)
                || (immediatePurchasePrice != null && (immediatePurchasePrice.scale() > 2
                || immediatePurchasePrice.compareTo(basePrice) <= 0))
                || (type == AuctionType.IMMEDIATE && immediatePurchasePrice == null)) {
            throw new BusinessRuleException("Invalid auction prices or dates");
        }
        if (type == AuctionType.DUTCH) {
            if (floorPrice == null || floorPrice.signum() <= 0 || floorPrice.scale() > 2
                    || floorPrice.compareTo(basePrice) >= 0 || immediatePurchasePrice != null) {
                throw new BusinessRuleException("Dutch auctions require a floor below their opening price");
            }
        } else if (floorPrice != null || (type == AuctionType.SEALED && immediatePurchasePrice != null)) {
            throw new BusinessRuleException("These prices are not available for this auction type");
        }
        Auction auction = new Auction(id, artworkId, type, status, basePrice, immediatePurchasePrice, start,
                endDate, currency, floorPrice);
        if (!auction.validPriceStep(basePrice)
                || (immediatePurchasePrice != null && !auction.validPriceStep(immediatePurchasePrice))
                || (floorPrice != null && !auction.validPriceStep(floorPrice))) {
            throw new BusinessRuleException("COP amounts must be multiples of 1,000");
        }
        return auction;
    }
}
