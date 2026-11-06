package com.underart.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AntiSnipingPolicyTest {

    private final Instant end = Instant.parse("2026-01-01T20:00:00Z");

    @Test
    void extendsWhenTheBidArrivesInsideTheWindow() {
        Instant bid = end.minusSeconds(30);
        assertThat(AntiSnipingPolicy.extendedEnd(end, bid)).contains(bid.plus(AntiSnipingPolicy.EXTENSION));
    }

    @Test
    void doesNotExtendEarlierBids() {
        assertThat(AntiSnipingPolicy.extendedEnd(end, end.minus(AntiSnipingPolicy.WINDOW))).isEmpty();
        assertThat(AntiSnipingPolicy.extendedEnd(end, end.minusSeconds(3600))).isEmpty();
    }

    @Test
    void neverShortensTheAuction() {
        assertThat(AntiSnipingPolicy.extendedEnd(end, end.minusSeconds(1)))
                .hasValueSatisfying(newEnd -> assertThat(newEnd).isAfter(end));
    }
}
