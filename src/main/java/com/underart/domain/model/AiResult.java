package com.underart.domain.model;

import com.underart.domain.exception.AiProviderException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record AiResult(String provider, BigDecimal suggestedPrice, List<String> tags, String rawJson) {

    public AiResult {
        if (suggestedPrice == null || suggestedPrice.signum() <= 0 || suggestedPrice.precision() > 12
                || tags == null || tags.size() > 20
                || tags.stream().anyMatch(tag -> tag == null || tag.isBlank() || tag.length() > 100)) {
            throw new AiProviderException("Invalid AI price or tags");
        }
        suggestedPrice = suggestedPrice.setScale(2, RoundingMode.HALF_UP);
        tags = List.copyOf(tags);
    }
}
