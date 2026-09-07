package com.underart.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("activity_logs")
public record ActivityLogDocument(@Id String id, String action, @Indexed Long userId, String detail, Instant createdAt) {

    public static ActivityLogDocument of(String action, Long userId, String detail) {
        return new ActivityLogDocument(null, action, userId, detail, Instant.now());
    }
}
