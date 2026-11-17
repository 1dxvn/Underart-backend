package com.underart.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BidIncrementPolicy {

    public static final BigDecimal MINIMUM_INCREMENT = new BigDecimal("1.00");
    public static final BigDecimal INCREMENT_RATE = new BigDecimal("0.05");

    private BidIncrementPolicy() {
    }

    public static BigDecimal minimumNextBid(BigDecimal basePrice, BigDecimal highestBid) {
        if (highestBid == null) {
            return basePrice.setScale(2, RoundingMode.CEILING);
        }
        BigDecimal increment = highestBid.multiply(INCREMENT_RATE).max(MINIMUM_INCREMENT);
        return highestBid.add(increment).setScale(2, RoundingMode.CEILING);
    }
}
