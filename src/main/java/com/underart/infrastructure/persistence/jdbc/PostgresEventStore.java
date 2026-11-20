package com.underart.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.underart.domain.model.ActivityLog;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.domain.port.out.NotificationRepositoryPort;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.infrastructure.persistence.AfterCommit;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("postgres-events")
@Slf4j
public class PostgresEventStore implements ActivityLogRepositoryPort, AiGenerationRepositoryPort,
        NotificationSenderPort, NotificationRepositoryPort {

    private static final TypeReference<Map<String, Object>> METADATA = new TypeReference<>() {};
    private static final TypeReference<Map<String, String>> NOTIFICATION_DATA = new TypeReference<>() {};

    private final JdbcClient db;
    private final ObjectMapper json;
    private final TransactionTemplate eventTransaction;

    public PostgresEventStore(JdbcClient db, ObjectMapper json, PlatformTransactionManager transactions) {
        this.db = db;
        this.json = json;
        this.eventTransaction = new TransactionTemplate(transactions);
        this.eventTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void log(String eventType, String description, UUID userId, Map<String, Object> metadata) {
        recordAfterCommit(eventType, userId, () -> db.sql("""
                INSERT INTO activity_events (id, event_type, description, user_id, metadata, created_at)
                VALUES (:id, :event, :description, :userId, CAST(:metadata AS jsonb), CURRENT_TIMESTAMP)
                """).param("id", UUID.randomUUID().toString()).param("event", eventType)
                .param("description", description).param("userId", userId)
                .param("metadata", encode(metadata)).update());
    }

    @Override
    public PageResult<ActivityLog> findByUserId(UUID userId, PageQuery page) {
        long total = db.sql("SELECT count(*) FROM activity_events WHERE user_id = :userId")
                .param("userId", userId).query(Long.class).single();
        var items = db.sql("""
                SELECT id, event_type, description, metadata, created_at FROM activity_events
                WHERE user_id = :userId ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset
                """).param("userId", userId).param("limit", page.size()).param("offset", offset(page))
                .query((row, index) -> new ActivityLog(row.getString("id"), row.getString("event_type"),
                        row.getString("description"), userId, decode(row.getString("metadata"), METADATA),
                        row.getObject("created_at", OffsetDateTime.class).toInstant())).list();
        return new PageResult<>(items, page.page(), page.size(), total);
    }

    @Override
    public void save(AiGenerationRecord record) {
        db.sql("""
                INSERT INTO ai_generations (provider, artwork_title, suggested_price, tags, raw_response, created_at)
                VALUES (:provider, :title, :price, CAST(:tags AS jsonb), :response, CURRENT_TIMESTAMP)
                """).param("provider", record.provider()).param("title", record.artworkTitle())
                .param("price", record.suggestedPrice()).param("tags", encode(record.tags()))
                .param("response", record.rawResponse()).update();
    }

    @Override
    public void send(Notification notification) {
        String id = notification.id() == null ? UUID.randomUUID().toString() : notification.id();
        recordAfterCommit(notification.type().name(), notification.userId(), () -> db.sql("""
                INSERT INTO user_notifications (id, user_id, notification_type, title, message, data,
                                                is_read, created_at)
                VALUES (:id, :userId, :type, :title, :message, CAST(:data AS jsonb), :read, :createdAt)
                """).param("id", id).param("userId", notification.userId()).param("type", notification.type().name())
                .param("title", notification.title()).param("message", notification.message())
                .param("data", encode(notification.data())).param("read", notification.read())
                .param("createdAt", notification.createdAt().atOffset(ZoneOffset.UTC)).update());
    }

    @Override
    public PageResult<Notification> findByUser(UUID userId, boolean unreadOnly, PageQuery page) {
        long total = db.sql("""
                SELECT count(*) FROM user_notifications WHERE user_id = :userId AND (NOT :unreadOnly OR NOT is_read)
                """).param("userId", userId).param("unreadOnly", unreadOnly).query(Long.class).single();
        var items = db.sql("""
                SELECT id, notification_type, title, message, data, is_read, created_at FROM user_notifications
                WHERE user_id = :userId AND (NOT :unreadOnly OR NOT is_read)
                ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset
                """).param("userId", userId).param("unreadOnly", unreadOnly)
                .param("limit", page.size()).param("offset", offset(page))
                .query((row, index) -> new Notification(row.getString("id"), userId,
                        NotificationType.valueOf(row.getString("notification_type")), row.getString("title"),
                        row.getString("message"), decode(row.getString("data"), NOTIFICATION_DATA),
                        row.getBoolean("is_read"), row.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
        return new PageResult<>(items, page.page(), page.size(), total);
    }

    @Override
    public long countUnread(UUID userId) {
        return db.sql("SELECT count(*) FROM user_notifications WHERE user_id = :userId AND NOT is_read")
                .param("userId", userId).query(Long.class).single();
    }

    @Override
    public boolean markRead(String notificationId, UUID userId) {
        return db.sql("UPDATE user_notifications SET is_read = true WHERE id = :id AND user_id = :userId")
                .param("id", notificationId).param("userId", userId).update() > 0;
    }

    @Override
    public long markAllRead(UUID userId) {
        return db.sql("UPDATE user_notifications SET is_read = true WHERE user_id = :userId AND NOT is_read")
                .param("userId", userId).update();
    }

    private void recordAfterCommit(String event, UUID userId, Runnable record) {
        AfterCommit.run(() -> {
            try {
                eventTransaction.executeWithoutResult(status -> record.run());
            } catch (RuntimeException ex) {
                log.warn("Could not record event {} for {}", event, userId, ex);
            }
        });
    }

    private long offset(PageQuery page) {
        return (long) page.page() * page.size();
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not encode event payload", ex);
        }
    }

    private <T> T decode(String value, TypeReference<T> type) {
        if (value == null) {
            return null;
        }
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read event payload", ex);
        }
    }
}
