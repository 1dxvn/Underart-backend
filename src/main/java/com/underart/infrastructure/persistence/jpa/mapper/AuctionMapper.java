package com.underart.infrastructure.persistence.jpa.mapper;

import com.underart.domain.model.Auction;
import com.underart.infrastructure.persistence.jpa.entity.ArtworkEntity;
import com.underart.infrastructure.persistence.jpa.entity.AuctionEntity;

public final class AuctionMapper {

    private AuctionMapper() {
    }

    public static Auction toDomain(AuctionEntity e) {
        return new Auction(e.getId(), e.getArtwork().getId(), e.getType(), e.getStatus(),
                e.getBasePrice(), e.getImmediatePurchasePrice(), e.getStartDate(), e.getEndDate());
    }

    public static AuctionEntity toEntity(Auction a) {
        return AuctionEntity.builder()
                .id(a.id())
                .artwork(ArtworkEntity.builder().id(a.artworkId()).build())
                .type(a.type())
                .status(a.status())
                .basePrice(a.basePrice())
                .immediatePurchasePrice(a.immediatePurchasePrice())
                .startDate(a.startDate())
                .endDate(a.endDate())
                .build();
    }
}
