package com.underart.infrastructure.persistence.jpa.mapper;

import com.underart.domain.model.Artwork;
import com.underart.infrastructure.persistence.jpa.entity.ArtworkEntity;
import com.underart.infrastructure.persistence.jpa.entity.UserEntity;
import java.util.ArrayList;

public final class ArtworkMapper {

    private ArtworkMapper() {
    }

    public static Artwork toDomain(ArtworkEntity e) {
        return new Artwork(e.getId(), e.getArtist().getId(), e.getTitle(), e.getDescription(),
                e.getTechnique(), e.getWeightKg(), e.getDimensions(), e.getSuggestedPrice(),
                e.getTags() == null ? null : new ArrayList<>(e.getTags()), e.getCreatedAt());
    }

    public static ArtworkEntity toEntity(Artwork a) {
        return ArtworkEntity.builder()
                .id(a.id())
                .artist(UserEntity.builder().id(a.artistId()).build())
                .title(a.title())
                .description(a.description())
                .technique(a.technique())
                .weightKg(a.weightKg())
                .dimensions(a.dimensions())
                .suggestedPrice(a.suggestedPrice())
                .tags(a.tags() == null ? null : new ArrayList<>(a.tags()))
                .createdAt(a.createdAt())
                .build();
    }
}
