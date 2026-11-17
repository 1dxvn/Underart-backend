package com.underart.infrastructure.persistence.jpa.mapper;

import com.underart.domain.model.RefreshToken;
import com.underart.infrastructure.persistence.jpa.entity.RefreshTokenEntity;

public final class RefreshTokenMapper {

    private RefreshTokenMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(), e.getFamilyId(), e.getExpiresAt(),
                e.getCreatedAt(), e.getRevokedAt(), e.getReplacedBy());
    }

    public static RefreshTokenEntity toEntity(RefreshToken t) {
        return RefreshTokenEntity.builder()
                .id(t.id())
                .userId(t.userId())
                .tokenHash(t.tokenHash())
                .familyId(t.familyId())
                .expiresAt(t.expiresAt())
                .createdAt(t.createdAt())
                .revokedAt(t.revokedAt())
                .replacedBy(t.replacedBy())
                .build();
    }
}
