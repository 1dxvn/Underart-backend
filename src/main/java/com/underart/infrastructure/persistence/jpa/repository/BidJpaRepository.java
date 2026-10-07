package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.BidEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidJpaRepository extends JpaRepository<BidEntity, UUID> {

    List<BidEntity> findByAuctionIdOrderByBidTimeDesc(UUID auctionId);

    Optional<BidEntity> findFirstByAuctionIdOrderByAmountDesc(UUID auctionId);
}
