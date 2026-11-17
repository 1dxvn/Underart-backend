package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.ArtworkEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkJpaRepository extends JpaRepository<ArtworkEntity, UUID>,
        JpaSpecificationExecutor<ArtworkEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ArtworkEntity a where a.id = :id")
    Optional<ArtworkEntity> findByIdForUpdate(@Param("id") UUID id);

    List<ArtworkEntity> findByArtistId(UUID artistId);

    long countByArtistId(UUID artistId);
}
