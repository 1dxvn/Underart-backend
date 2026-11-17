package com.underart.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Notification(
        String id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        Map<String, String> data,
        boolean read,
        Instant createdAt) {

    public Notification {
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static Notification create(UUID userId, NotificationType type, String title, String message,
                                      Map<String, String> data) {
        return new Notification(null, userId, type, title, message, data, false, Instant.now());
    }
}
