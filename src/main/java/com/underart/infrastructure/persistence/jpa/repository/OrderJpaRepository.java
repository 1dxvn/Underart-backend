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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByWinnerId(UUID winnerId, Pageable pageable);

    @Query("select o from OrderEntity o where o.auction.artwork.artist.id = :artistId"
            + " and (not o.checkout or o.paymentStatus = com.underart.domain.model.PaymentStatus.PAID)")
    Page<OrderEntity> findSales(@Param("artistId") UUID artistId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select count(o) > 0 from OrderEntity o where o.auction.artwork.id = :artworkId "
            + "and ((not o.checkout and o.paymentStatus <> com.underart.domain.model.PaymentStatus.EXPIRED)"
            + " or o.paymentStatus in (com.underart.domain.model.PaymentStatus.PROCESSING,"
            + " com.underart.domain.model.PaymentStatus.PAID))")
    boolean reservesArtwork(@Param("artworkId") UUID artworkId);

    @Query("select o.id from OrderEntity o where o.paymentDueAt <= :now and o.paymentStatus in "
            + "(com.underart.domain.model.PaymentStatus.PENDING, com.underart.domain.model.PaymentStatus.FAILED)")
    List<UUID> findUnpaidExpiredIds(@Param("now") Instant now);

    @Query("select o.auction.id from OrderEntity o where o.id = :id and o.checkout")
    Optional<UUID> findCheckoutAuctionId(@Param("id") UUID id);

    @Query("select count(o) > 0 from OrderEntity o where o.auction.id = :id and o.checkout"
            + " and o.paymentStatus = com.underart.domain.model.PaymentStatus.PROCESSING")
    boolean hasProcessingCheckout(@Param("id") UUID id);

    @Modifying(flushAutomatically = true)
    @Query("update OrderEntity o set o.paymentStatus = com.underart.domain.model.PaymentStatus.EXPIRED"
            + " where o.auction.id = :id and o.checkout and o.paymentStatus in"
            + " (com.underart.domain.model.PaymentStatus.PENDING, com.underart.domain.model.PaymentStatus.FAILED)")
    void expireOpenCheckouts(@Param("id") UUID id);

    @Query("select count(o) > 0 from OrderEntity o where o.auction.id = :id and not o.checkout")
    boolean existsByAuctionId(@Param("id") UUID auctionId);

    long countByWinnerId(UUID winnerId);

    @Query("select count(o), coalesce(sum(case when o.paymentStatus = com.underart.domain.model.PaymentStatus.PAID "
            + "then o.subtotal else 0 end), 0) from OrderEntity o where o.auction.artwork.artist.id = :seller"
            + " and (not o.checkout or o.paymentStatus = com.underart.domain.model.PaymentStatus.PAID)")
    List<Object[]> salesStatsForSeller(@Param("seller") UUID sellerId);
}
