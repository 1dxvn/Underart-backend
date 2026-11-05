package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.domain.model.AuctionStatus;
import com.underart.infrastructure.persistence.jpa.entity.AuctionEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionJpaRepository extends JpaRepository<AuctionEntity, UUID>,
        JpaSpecificationExecutor<AuctionEntity> {

    List<AuctionEntity> findByStatus(AuctionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AuctionEntity a where a.id = :id")
    Optional<AuctionEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select a.id from AuctionEntity a where a.status = :status and a.endDate < :now")
    List<UUID> findIdsByStatusAndEndDateBefore(@Param("status") AuctionStatus status, @Param("now") Instant now);

    boolean existsByArtworkId(UUID artworkId);

    boolean existsByArtworkIdAndStatus(UUID artworkId, AuctionStatus status);
}
