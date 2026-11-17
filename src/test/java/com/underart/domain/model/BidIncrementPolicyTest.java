package com.underart.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BidIncrementPolicyTest {

    @ParameterizedTest(name = "base {0}, highest {1} -> minimum {2}")
    @CsvSource(nullValues = "null", value = {
            "100, null, 100.00",
            "99.999, null, 100.00",
            "100, 150, 157.50",
            "10, 12, 13.00",
            "100, 200, 210.00",
            "100, 101.01, 106.07"
    })
    void computesTheMinimumNextBid(String base, String highest, String expected) {
        BigDecimal minimum = BidIncrementPolicy.minimumNextBid(new BigDecimal(base),
                highest == null ? null : new BigDecimal(highest));
        assertThat(minimum).isEqualByComparingTo(expected);
        assertThat(minimum.scale()).isEqualTo(2);
    }
}
