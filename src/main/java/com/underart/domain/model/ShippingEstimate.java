package com.underart.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class ShippingEstimate {

    private ShippingEstimate() {}

    public static BigDecimal forWeight(String currency, BigDecimal kilograms) {
        boolean cop = "COP".equals(currency.toUpperCase(Locale.ROOT));
        BigDecimal base = cop ? new BigDecimal("15000") : new BigDecimal("15");
        BigDecimal perExtraKg = cop ? new BigDecimal("3000") : new BigDecimal("3");
        BigDecimal extra = kilograms == null ? BigDecimal.ZERO
                : kilograms.setScale(0, RoundingMode.CEILING).subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
        return base.add(extra.multiply(perExtraKg));
    }
}
