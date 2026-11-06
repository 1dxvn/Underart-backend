package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.ArtworkMediaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkMediaJpaRepository extends JpaRepository<ArtworkMediaEntity, UUID> {

    List<ArtworkMediaEntity> findByArtworkIdOrderByPositionAsc(UUID artworkId);

    long countByArtworkId(UUID artworkId);

    @Query("select m.artwork.id, m.url from ArtworkMediaEntity m where m.artwork.id in :ids "
            + "and m.kind = com.underart.domain.model.MediaKind.IMAGE order by m.position asc")
    List<Object[]> imageUrlsByArtworkIds(@Param("ids") Collection<UUID> artworkIds);
}
