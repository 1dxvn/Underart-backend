package com.underart.infrastructure.persistence.jpa.mapper;

import com.underart.domain.model.ArtworkMedia;
import com.underart.infrastructure.persistence.jpa.entity.ArtworkEntity;
import com.underart.infrastructure.persistence.jpa.entity.ArtworkMediaEntity;

public final class ArtworkMediaMapper {

    private ArtworkMediaMapper() {
    }

    public static ArtworkMedia toDomain(ArtworkMediaEntity e) {
        return new ArtworkMedia(e.getId(), e.getArtwork().getId(), e.getKind(), e.getUrl(),
                e.getStorageKey(), e.getContentType(), e.getSizeBytes(), e.getPosition(), e.getCreatedAt());
    }

    public static ArtworkMediaEntity toEntity(ArtworkMedia m) {
        return ArtworkMediaEntity.builder()
                .id(m.id())
                .artwork(ArtworkEntity.builder().id(m.artworkId()).build())
                .kind(m.kind())
                .url(m.url())
                .storageKey(m.storageKey())
                .contentType(m.contentType())
                .sizeBytes(m.sizeBytes())
                .position(m.position())
                .createdAt(m.createdAt())
                .build();
    }
}
