package com.underart.application.dto;

import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
        String id,
        NotificationType type,
        String title,
        String message,
        Map<String, String> data,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.id(), n.type(), n.title(), n.message(), n.data(), n.read(), n.createdAt());
    }
}
