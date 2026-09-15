package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.domain.model.AuctionStatus;
import com.underart.infrastructure.persistence.jpa.entity.AuctionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionJpaRepository extends JpaRepository<AuctionEntity, UUID> {

    List<AuctionEntity> findByStatus(AuctionStatus status);
}
