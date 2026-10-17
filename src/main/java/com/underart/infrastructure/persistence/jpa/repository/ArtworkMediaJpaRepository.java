package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.ArtworkMediaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtworkMediaJpaRepository extends JpaRepository<ArtworkMediaEntity, UUID> {

    List<ArtworkMediaEntity> findByArtworkIdOrderByPositionAsc(UUID artworkId);

    long countByArtworkId(UUID artworkId);
}
