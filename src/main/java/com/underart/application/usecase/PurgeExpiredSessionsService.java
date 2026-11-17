package com.underart.application.usecase;

import com.underart.domain.port.in.PurgeExpiredSessionsUseCase;
import com.underart.domain.port.out.RefreshTokenRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PurgeExpiredSessionsService implements PurgeExpiredSessionsUseCase {

    static final Duration RETENTION = Duration.ofDays(7);

    private final RefreshTokenRepositoryPort refreshTokens;

    @Override
    public long purgeExpired() {
        return refreshTokens.deleteExpiredBefore(Instant.now().minus(RETENTION));
    }
}
