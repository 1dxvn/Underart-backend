package com.underart.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class AntiSnipingPolicy {

    public static final Duration WINDOW = Duration.ofMinutes(2);
    public static final Duration EXTENSION = Duration.ofMinutes(2);

    private AntiSnipingPolicy() {
    }

    public static Optional<Instant> extendedEnd(Instant endDate, Instant bidTime) {
        if (Duration.between(bidTime, endDate).compareTo(WINDOW) >= 0) {
            return Optional.empty();
        }
        Instant candidate = bidTime.plus(EXTENSION);
        return candidate.isAfter(endDate) ? Optional.of(candidate) : Optional.empty();
    }
}
