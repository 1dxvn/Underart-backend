package com.underart.application.usecase;

import com.underart.domain.exception.TooManyRequestsException;
import com.underart.domain.port.out.RateLimiterPort;
import java.time.Duration;
import java.util.UUID;

final class AiUsagePolicy {

    static final int MAX_USES_PER_WINDOW = 10;
    static final Duration WINDOW = Duration.ofHours(1);

    private AiUsagePolicy() {
    }

    static void consume(RateLimiterPort rateLimiter, UUID userId) {
        String key = "ai:" + userId;
        rateLimiter.blockedFor(key, MAX_USES_PER_WINDOW, WINDOW).ifPresent(wait -> {
            throw new TooManyRequestsException("Alcanzaste el límite de " + MAX_USES_PER_WINDOW
                    + " usos de IA por hora. Inténtalo de nuevo en " + ((wait.toSeconds() + 59) / 60) + " minutos",
                    wait.toSeconds());
        });
        rateLimiter.hit(key, WINDOW);
    }
}
