package com.underart.domain.port.out;

import com.underart.domain.model.ArtworkMedia;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkMediaRepositoryPort {

    ArtworkMedia save(ArtworkMedia media);

    List<ArtworkMedia> saveAll(List<ArtworkMedia> media);

    Optional<ArtworkMedia> findById(UUID id);

    List<ArtworkMedia> findByArtworkId(UUID artworkId);

    long countByArtworkId(UUID artworkId);

    void deleteById(UUID id);

    Map<UUID, String> coverUrlsByArtworkIds(Collection<UUID> artworkIds);
}
