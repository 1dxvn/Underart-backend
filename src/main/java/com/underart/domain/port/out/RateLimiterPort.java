package com.underart.domain.port.out;

import java.time.Duration;
import java.util.Optional;

public interface RateLimiterPort {

    Optional<Duration> blockedFor(String key, int limit, Duration window);

    void hit(String key, Duration window);

    void reset(String key);
}
