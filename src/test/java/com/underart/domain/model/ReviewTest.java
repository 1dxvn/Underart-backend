package com.underart.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.underart.domain.exception.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ReviewTest {

    private Review review(int rating, String comment) {
        return new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rating,
                comment, Instant.now());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void rejectsRatingsOutsideOneToFive(int rating) {
        assertThatThrownBy(() -> review(rating, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void normalizesBlankCommentsAndLimitsLength() {
        assertThat(review(5, "   ").comment()).isNull();
        assertThat(review(4, "  Excellent  ").comment()).isEqualTo("Excellent");
        assertThatThrownBy(() -> review(3, "x".repeat(1001))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void ratingSummaryRoundsAndHandlesNoReviews() {
        assertThat(RatingSummary.of(null, 0)).isEqualTo(RatingSummary.NONE);
        assertThat(RatingSummary.of(4.666666, 3).average()).isEqualByComparingTo("4.67");
    }
}
