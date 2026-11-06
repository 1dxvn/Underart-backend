package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.RefreshToken;
import com.underart.domain.port.out.RefreshTokenRepositoryPort;
import com.underart.infrastructure.persistence.jpa.mapper.RefreshTokenMapper;
import com.underart.infrastructure.persistence.jpa.repository.RefreshTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return RefreshTokenMapper.toDomain(repository.save(RefreshTokenMapper.toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshTokenMapper::toDomain);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        repository.revokeFamily(familyId, now);
    }

    @Override
    public void revokeAllForUser(UUID userId, Instant now) {
        repository.revokeAllForUser(userId, now);
    }

    @Override
    public long deleteExpiredBefore(Instant cutoff) {
        return repository.deleteExpiredBefore(cutoff);
    }
}
