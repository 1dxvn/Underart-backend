package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.ArtworkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtworkJpaRepository extends JpaRepository<ArtworkEntity, UUID> {

    List<ArtworkEntity> findByArtistId(UUID artistId);
}
