package com.underart.domain.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface AiGenerationRepositoryPort {

    void save(AiGenerationRecord record);

    record AiGenerationRecord(
            String provider,
            String artworkTitle,
            BigDecimal suggestedPrice,
            List<String> tags,
            String rawResponse) {
    }
}
