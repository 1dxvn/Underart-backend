package com.underart.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterAdapterTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final InMemoryRateLimiterAdapter limiter = new InMemoryRateLimiterAdapter(clock);

    @Test
    void blocksAfterTheLimitAndReportsTheRemainingTime() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.blockedFor("k", 3, WINDOW)).isEmpty();
            limiter.hit("k", WINDOW);
            clock.advance(Duration.ofMinutes(1));
        }

        assertThat(limiter.blockedFor("k", 3, WINDOW)).contains(Duration.ofMinutes(12));
    }

    @Test
    void oldHitsLeaveTheWindow() {
        limiter.hit("k", WINDOW);
        limiter.hit("k", WINDOW);
        clock.advance(WINDOW.plusSeconds(1));

        assertThat(limiter.blockedFor("k", 2, WINDOW)).isEmpty();
    }

    @Test
    void resetClearsTheKeyAndKeysAreIndependent() {
        limiter.hit("a", WINDOW);
        limiter.hit("a", WINDOW);
        limiter.hit("b", WINDOW);
        limiter.reset("a");

        assertThat(limiter.blockedFor("a", 1, WINDOW)).isEmpty();
        assertThat(limiter.blockedFor("b", 1, WINDOW)).isPresent();
    }

    @Test
    void evictionDropsStaleKeys() {
        limiter.hit("k", WINDOW);
        clock.advance(Duration.ofDays(2));
        limiter.evictStaleKeys();

        assertThat(limiter.blockedFor("k", 1, WINDOW)).isEmpty();
    }
}
