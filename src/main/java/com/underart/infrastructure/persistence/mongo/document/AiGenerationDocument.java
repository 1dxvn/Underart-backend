package com.underart.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("ai_generations")
public record AiGenerationDocument(@Id String id, String provider, String prompt, String output,
                                   long latencyMs, Instant createdAt) {

    public static AiGenerationDocument of(String provider, String prompt, String output, long latencyMs) {
        return new AiGenerationDocument(null, provider, prompt, output, latencyMs, Instant.now());
    }
}
