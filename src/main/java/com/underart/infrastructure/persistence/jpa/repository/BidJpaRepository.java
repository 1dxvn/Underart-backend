package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.BidEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidJpaRepository extends JpaRepository<BidEntity, UUID> {

    List<BidEntity> findByAuctionIdOrderByBidTimeDesc(UUID auctionId);

    Optional<BidEntity> findFirstByAuctionIdOrderByAmountDescBidTimeAscIdAsc(UUID auctionId);

    Page<BidEntity> findByAuctionId(UUID auctionId, Pageable pageable);

    Page<BidEntity> findByBuyerId(UUID buyerId, Pageable pageable);

    boolean existsByAuctionIdAndBuyerId(UUID auctionId, UUID buyerId);

    long countByAuctionId(UUID auctionId);

    @Query("select b.auction.id, max(b.amount), count(b) from BidEntity b "
            + "where b.auction.id in :ids group by b.auction.id")
    List<Object[]> statsByAuctionIds(@Param("ids") Collection<UUID> auctionIds);
}
