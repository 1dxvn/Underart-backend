package com.underart.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        UUID familyId,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt,
        UUID replacedBy) {

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public RefreshToken revokedBy(UUID replacement, Instant now) {
        return new RefreshToken(id, userId, tokenHash, familyId, expiresAt, createdAt, now, replacement);
    }
}
