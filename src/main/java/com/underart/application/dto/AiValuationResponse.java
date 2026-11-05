package com.underart.application.dto;

import com.underart.domain.model.AiResult;
import java.math.BigDecimal;
import java.util.List;

public record AiValuationResponse(String provider, BigDecimal suggestedPrice, List<String> tags) {

    public static AiValuationResponse from(AiResult result) {
        return new AiValuationResponse(result.provider(), result.suggestedPrice(), result.tags());
    }
}
