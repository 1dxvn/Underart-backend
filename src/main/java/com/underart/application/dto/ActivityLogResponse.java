package com.underart.application.dto;

import com.underart.domain.model.ActivityLog;
import java.time.Instant;
import java.util.Map;

public record ActivityLogResponse(String eventType, String description, Map<String, Object> metadata,
                                  Instant createdAt) {

    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(log.eventType(), log.description(), log.metadata(), log.createdAt());
    }
}
