package com.underart.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ActivityLog(
        String id,
        String eventType,
        String description,
        UUID userId,
        Map<String, Object> metadata,
        Instant createdAt) {
}
