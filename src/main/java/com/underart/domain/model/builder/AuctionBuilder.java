package com.underart.domain.model.builder;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;

public final class AuctionBuilder {
    private Long artworkId;
    private Long sellerId;
    private AuctionType type = AuctionType.ENGLISH;
    private BigDecimal startingPrice;
    private BigDecimal buyNowPrice;
    private Instant endsAt;

    private AuctionBuilder() {}

    public static AuctionBuilder anAuction() { return new AuctionBuilder(); }

    public AuctionBuilder artworkId(Long v) { artworkId = v; return this; }
    public AuctionBuilder sellerId(Long v) { sellerId = v; return this; }
    public AuctionBuilder type(AuctionType v) { if (v != null) type = v; return this; }
    public AuctionBuilder startingPrice(BigDecimal v) { startingPrice = v; return this; }
    public AuctionBuilder buyNowPrice(BigDecimal v) { buyNowPrice = v; return this; }
    public AuctionBuilder endsAt(Instant v) { endsAt = v; return this; }

    public AuctionBuilder validate() {
        if (endsAt == null || !endsAt.isAfter(Instant.now())) throw new BusinessRuleException("La fecha de cierre debe ser futura");
        if (type == AuctionType.DIRECT_SALE && buyNowPrice == null)
            throw new BusinessRuleException("La venta directa requiere precio de compra");
        if (initialPrice() == null || initialPrice().signum() <= 0) throw new BusinessRuleException("El precio inicial debe ser positivo");
        if (type == AuctionType.ENGLISH && buyNowPrice != null && buyNowPrice.compareTo(startingPrice) <= 0)
            throw new BusinessRuleException("El precio de compra directa debe superar el inicial");
        return this;
    }

    public Auction build() {
        if (artworkId == null || sellerId == null) throw new BusinessRuleException("La subasta requiere obra y vendedor");
        validate();
        return new Auction(null, artworkId, sellerId, type, AuctionStatus.ACTIVE, initialPrice(), buyNowPrice, null, endsAt);
    }

    private BigDecimal initialPrice() { return type == AuctionType.DIRECT_SALE ? buyNowPrice : startingPrice; }
}
