package com.underart.domain.port.out;

import com.underart.domain.model.Notification;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.UUID;

public interface NotificationRepositoryPort {

    PageResult<Notification> findByUser(UUID userId, boolean unreadOnly, PageQuery page);

    long countUnread(UUID userId);

    boolean markRead(String notificationId, UUID userId);

    long markAllRead(UUID userId);
}
