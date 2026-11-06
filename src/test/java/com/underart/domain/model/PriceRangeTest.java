package com.underart.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.underart.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PriceRangeTest {

    @Test
    void acceptsOpenAndClosedRanges() {
        assertThatCode(() -> new PriceRange(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> new PriceRange(BigDecimal.ONE, BigDecimal.ONE)).doesNotThrowAnyException();
        assertThatCode(() -> new PriceRange(null, BigDecimal.TEN)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvertedOrNegativeRanges() {
        assertThatThrownBy(() -> new PriceRange(BigDecimal.TEN, BigDecimal.ONE)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new PriceRange(new BigDecimal("-1"), null)).isInstanceOf(BusinessRuleException.class);
    }
}
