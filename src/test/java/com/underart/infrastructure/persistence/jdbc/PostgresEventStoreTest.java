package com.underart.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.underart.domain.model.ActivityLog;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.PageQuery;
import com.underart.domain.port.out.AiGenerationRepositoryPort.AiGenerationRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "auctions.closing.enabled=false")
@ActiveProfiles({"dev", "postgres-events"})
@Testcontainers(disabledWithoutDocker = true)
class PostgresEventStoreTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired PostgresEventStore events;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcClient db;

    @Test
    void recordsEventsOnlyAfterCommitAndKeepsNotificationsScopedToTheirOwner() {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var transaction = new TransactionTemplate(transactions);
        var notification = Notification.create(user, NotificationType.AUCTION_WON, "Won", "An artwork",
                Map.of("auction", "123"));

        transaction.executeWithoutResult(status -> {
            events.log("ROLLED_BACK", null, user, null);
            events.send(notification);
            status.setRollbackOnly();
        });
        assertThat(events.countUnread(user)).isZero();
        assertThat(events.findByUserId(user, PageQuery.first()).items()).isEmpty();

        transaction.executeWithoutResult(status -> {
            events.log("WON", "An artwork", user, Map.of("auction", "123"));
            events.send(notification);
            assertThat(events.countUnread(user)).isZero();
        });
        assertThat(events.findByUserId(user, PageQuery.first()).items())
                .singleElement().extracting(ActivityLog::metadata).isEqualTo(Map.of("auction", "123"));
        assertThat(events.countUnread(user)).isEqualTo(1);
        var page = events.findByUser(user, true, PageQuery.first());
        assertThat(page.totalItems()).isEqualTo(1);
        String id = page.items().get(0).id();
        assertThat(events.markRead(id, other)).isFalse();
        assertThat(events.markRead(id, user)).isTrue();
        assertThat(events.findByUser(user, true, PageQuery.first()).items()).isEmpty();
        assertThat(events.findByUser(user, false, PageQuery.first()).items())
                .singleElement().extracting(Notification::data).isEqualTo(Map.of("auction", "123"));

        events.send(notification);
        assertThat(events.markAllRead(user)).isEqualTo(1);
        assertThat(events.countUnread(user)).isZero();
    }

    @Test
    void persistsAiGenerationResultsInPostgres() {
        events.save(new AiGenerationRecord("groq", "Study", new BigDecimal("75.25"), List.of("ink"), "{}"));
        assertThat(db.sql("SELECT tags ->> 0 FROM ai_generations WHERE artwork_title = 'Study'")
                .query(String.class).single()).isEqualTo("ink");
    }
}
