package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByWinnerId(UUID winnerId, Pageable pageable);

    Page<OrderEntity> findByAuction_Artwork_Artist_Id(UUID artistId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select count(o) > 0 from OrderEntity o where o.auction.artwork.id = :artworkId "
            + "and o.paymentStatus <> com.underart.domain.model.PaymentStatus.EXPIRED")
    boolean reservesArtwork(@Param("artworkId") UUID artworkId);

    @Query("select o.id from OrderEntity o where o.paymentDueAt <= :now and o.paymentStatus in "
            + "(com.underart.domain.model.PaymentStatus.PENDING, com.underart.domain.model.PaymentStatus.FAILED)")
    List<UUID> findUnpaidExpiredIds(@Param("now") Instant now);

    boolean existsByAuctionId(UUID auctionId);

    long countByWinnerId(UUID winnerId);

    @Query("select count(o), coalesce(sum(case when o.paymentStatus = com.underart.domain.model.PaymentStatus.PAID "
            + "then o.subtotal else 0 end), 0) from OrderEntity o where o.auction.artwork.artist.id = :seller")
    List<Object[]> salesStatsForSeller(@Param("seller") UUID sellerId);
}
