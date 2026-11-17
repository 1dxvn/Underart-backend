package com.underart.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.AuctionSortField;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SortParamTest {

    private static final Map<String, AuctionSortField> ALLOWED = Map.of(
            "endDate", AuctionSortField.END_DATE, "basePrice", AuctionSortField.BASE_PRICE);

    @Test
    void usesTheDefaultWhenMissing() {
        assertThat(SortParam.parse(null, ALLOWED, AuctionSortField.END_DATE, true))
                .isEqualTo(new SortParam<>(AuctionSortField.END_DATE, true));
    }

    @Test
    void parsesFieldAndDirection() {
        assertThat(SortParam.parse("basePrice,DESC", ALLOWED, AuctionSortField.END_DATE, true))
                .isEqualTo(new SortParam<>(AuctionSortField.BASE_PRICE, false));
        assertThat(SortParam.parse("basePrice", ALLOWED, AuctionSortField.END_DATE, false).ascending()).isTrue();
    }

    @Test
    void rejectsUnknownFieldsOrDirections() {
        assertThatThrownBy(() -> SortParam.parse("password,asc", ALLOWED, AuctionSortField.END_DATE, true))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> SortParam.parse("basePrice,up", ALLOWED, AuctionSortField.END_DATE, true))
                .isInstanceOf(BusinessRuleException.class);
    }
}
