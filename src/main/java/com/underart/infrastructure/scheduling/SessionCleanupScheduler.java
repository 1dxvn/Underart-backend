package com.underart.infrastructure.scheduling;

import com.underart.domain.port.in.PurgeExpiredSessionsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupScheduler {

    private final PurgeExpiredSessionsUseCase purgeExpiredSessionsUseCase;

    @Scheduled(cron = "${auth.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    public void purgeExpiredSessions() {
        long deleted = purgeExpiredSessionsUseCase.purgeExpired();
        if (deleted > 0) {
            log.info("Eliminados {} refresh tokens caducados", deleted);
        }
    }
}
