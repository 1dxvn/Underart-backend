package com.underart.infrastructure.persistence.mongo.document;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@CompoundIndex(name = "user_read_created", def = "{'userId': 1, 'read': 1, 'createdAt': -1}")
public record NotificationDocument(
        @Id String id,
        String userId,
        String type,
        String title,
        String message,
        Map<String, String> data,
        boolean read,
        Instant createdAt) {
}
