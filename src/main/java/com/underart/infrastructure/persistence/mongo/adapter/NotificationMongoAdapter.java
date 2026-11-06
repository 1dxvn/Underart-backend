package com.underart.infrastructure.persistence.mongo.adapter;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.out.NotificationRepositoryPort;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.infrastructure.persistence.mongo.document.NotificationDocument;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMongoAdapter implements NotificationSenderPort, NotificationRepositoryPort {

    private final MongoTemplate mongo;

    @Override
    public void send(Notification notification) {
        try {
            mongo.insert(toDocument(notification));
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar la notificación {} para {}", notification.type(), notification.userId(), e);
        }
    }

    @Override
    public PageResult<Notification> findByUser(UUID userId, boolean unreadOnly, PageQuery page) {
        Query query = ownedBy(userId);
        if (unreadOnly) {
            query.addCriteria(where("read").is(false));
        }
        long total = mongo.count(query, NotificationDocument.class);
        query.with(PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Notification> items = mongo.find(query, NotificationDocument.class).stream().map(this::toDomain).toList();
        return new PageResult<>(items, page.page(), page.size(), total);
    }

    @Override
    public long countUnread(UUID userId) {
        return mongo.count(ownedBy(userId).addCriteria(where("read").is(false)), NotificationDocument.class);
    }

    @Override
    public boolean markRead(String notificationId, UUID userId) {
        Query query = ownedBy(userId).addCriteria(where("_id").is(notificationId));
        return mongo.updateFirst(query, Update.update("read", true), NotificationDocument.class).getMatchedCount() > 0;
    }

    @Override
    public long markAllRead(UUID userId) {
        Query query = ownedBy(userId).addCriteria(where("read").is(false));
        return mongo.updateMulti(query, Update.update("read", true), NotificationDocument.class).getModifiedCount();
    }

    private static Query ownedBy(UUID userId) {
        return Query.query(where("userId").is(userId.toString()));
    }

    private NotificationDocument toDocument(Notification n) {
        return new NotificationDocument(n.id(), n.userId().toString(), n.type().name(), n.title(), n.message(),
                n.data(), n.read(), n.createdAt());
    }

    private Notification toDomain(NotificationDocument d) {
        return new Notification(d.id(), UUID.fromString(d.userId()), NotificationType.valueOf(d.type()), d.title(),
                d.message(), d.data(), d.read(), d.createdAt());
    }
}
