package com.underart.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RatingSummary(BigDecimal average, long count) {

    public static final RatingSummary NONE = new RatingSummary(null, 0);

    public static RatingSummary of(Double average, long count) {
        if (count == 0 || average == null) {
            return NONE;
        }
        return new RatingSummary(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP), count);
    }
}
