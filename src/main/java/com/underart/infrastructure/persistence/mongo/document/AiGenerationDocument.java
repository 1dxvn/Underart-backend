package com.underart.infrastructure.persistence.mongo.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ai_generations")
public record AiGenerationDocument(
        @Id String id,
        String provider,
        String artworkTitle,
        BigDecimal suggestedPrice,
        List<String> tags,
        String rawResponse,
        Instant createdAt) {
}
