package com.underart.infrastructure.persistence.jpa.mapper;

import com.underart.domain.model.Bid;
import com.underart.infrastructure.persistence.jpa.entity.AuctionEntity;
import com.underart.infrastructure.persistence.jpa.entity.BidEntity;
import com.underart.infrastructure.persistence.jpa.entity.UserEntity;

public final class BidMapper {

    private BidMapper() {
    }

    public static Bid toDomain(BidEntity e) {
        return new Bid(e.getId(), e.getAuction().getId(), e.getBuyer().getId(),
                e.getAmount(), e.getBidTime());
    }

    public static BidEntity toEntity(Bid b) {
        return BidEntity.builder()
                .id(b.id())
                .auction(AuctionEntity.builder().id(b.auctionId()).build())
                .buyer(UserEntity.builder().id(b.buyerId()).build())
                .amount(b.amount())
                .bidTime(b.bidTime())
                .build();
    }
}
