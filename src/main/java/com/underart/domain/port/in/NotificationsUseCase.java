package com.underart.domain.port.in;

import com.underart.domain.model.Notification;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.UUID;

public interface NotificationsUseCase {

    PageResult<Notification> list(UUID userId, boolean unreadOnly, PageQuery page);

    long unreadCount(UUID userId);

    void markRead(UUID userId, String notificationId);

    void markAllRead(UUID userId);
}
