package com.underart.domain.model;

import com.underart.domain.exception.BusinessRuleException;
import java.math.BigDecimal;

public record PriceRange(BigDecimal min, BigDecimal max) {

    public static final PriceRange ANY = new PriceRange(null, null);

    public PriceRange {
        if (min != null && min.signum() < 0 || max != null && max.signum() < 0) {
            throw new BusinessRuleException("Prices must not be negative");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessRuleException("Minimum price must not exceed maximum price");
        }
    }
}
