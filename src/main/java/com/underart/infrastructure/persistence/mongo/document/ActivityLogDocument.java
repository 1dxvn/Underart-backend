package com.underart.infrastructure.persistence.mongo.document;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "activity_logs")
public record ActivityLogDocument(
        @Id String id,
        String eventType,
        String description,
        String userId,
        Map<String, Object> metadata,
        Instant createdAt) {
}
