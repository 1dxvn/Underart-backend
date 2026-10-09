package com.underart.domain.model.builder;

import com.underart.domain.model.Auction;
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

    public Auction build() {
        if (artworkId == null || basePrice == null || endDate == null) {
            throw new IllegalStateException("Faltan artwork, basePrice o endDate");
        }
        Instant start = startDate != null ? startDate : Instant.now();
        return new Auction(id, artworkId, type, status, basePrice, immediatePurchasePrice, start, endDate);
    }
}
