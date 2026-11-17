package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.FavoriteEntity;
import com.underart.infrastructure.persistence.jpa.entity.FavoriteId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteJpaRepository extends JpaRepository<FavoriteEntity, FavoriteId> {

    long countByIdArtworkId(UUID artworkId);

    Page<FavoriteEntity> findByIdUserId(UUID userId, Pageable pageable);
}
