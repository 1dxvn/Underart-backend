package com.underart.infrastructure.ratelimit;

import com.underart.domain.port.out.RateLimiterPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiterAdapter implements RateLimiterPort {

    private static final Duration MAX_WINDOW = Duration.ofDays(1);

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public InMemoryRateLimiterAdapter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiterAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<Duration> blockedFor(String key, int limit, Duration window) {
        Deque<Instant> log = hits.get(key);
        if (log == null) {
            return Optional.empty();
        }
        synchronized (log) {
            Instant now = clock.instant();
            prune(log, now.minus(window));
            if (log.size() < limit) {
                return Optional.empty();
            }
            return Optional.of(Duration.between(now, log.peekFirst().plus(window)));
        }
    }

    @Override
    public void hit(String key, Duration window) {
        Deque<Instant> log = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (log) {
            Instant now = clock.instant();
            prune(log, now.minus(window));
            log.addLast(now);
        }
    }

    @Override
    public void reset(String key) {
        hits.remove(key);
    }

    @Scheduled(fixedDelay = 600_000)
    void evictStaleKeys() {
        Instant cutoff = clock.instant().minus(MAX_WINDOW);
        hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), cutoff);
                return entry.getValue().isEmpty();
            }
        });
    }

    private static void prune(Deque<Instant> log, Instant cutoff) {
        while (!log.isEmpty() && log.peekFirst().isBefore(cutoff)) {
            log.pollFirst();
        }
    }
}
