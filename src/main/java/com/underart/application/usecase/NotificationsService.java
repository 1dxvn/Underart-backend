package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Notification;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.NotificationsUseCase;
import com.underart.domain.port.out.NotificationRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationsService implements NotificationsUseCase {

    private final NotificationRepositoryPort notifications;

    @Override
    public PageResult<Notification> list(UUID userId, boolean unreadOnly, PageQuery page) {
        return notifications.findByUser(userId, unreadOnly, page);
    }

    @Override
    public long unreadCount(UUID userId) {
        return notifications.countUnread(userId);
    }

    @Override
    public void markRead(UUID userId, String notificationId) {
        if (!notifications.markRead(notificationId, userId)) {
            throw new ResourceNotFoundException("Notificación no encontrada: " + notificationId);
        }
    }

    @Override
    public void markAllRead(UUID userId) {
        notifications.markAllRead(userId);
    }
}
