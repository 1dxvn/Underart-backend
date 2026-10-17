package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.OrderEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByWinnerId(UUID winnerId, Pageable pageable);

    Page<OrderEntity> findByAuction_Artwork_Artist_Id(UUID artistId, Pageable pageable);

    boolean existsByAuctionId(UUID auctionId);
}
