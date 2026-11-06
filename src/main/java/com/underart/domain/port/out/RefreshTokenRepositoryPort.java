package com.underart.domain.port.out;

import com.underart.domain.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeFamily(UUID familyId, Instant now);

    void revokeAllForUser(UUID userId, Instant now);

    long deleteExpiredBefore(Instant cutoff);
}
