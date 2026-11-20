package com.underart.infrastructure.persistence.jdbc;

import com.underart.domain.port.out.OrderMessageRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcOrderMessageRepository implements OrderMessageRepository {
    private final JdbcClient db;

    @Override
    public List<Message> list(UUID orderId, Long after) {
        if (after == null) {
            return db.sql("""
                    SELECT * FROM (SELECT * FROM order_messages WHERE order_id = :orderId
                    ORDER BY id DESC LIMIT 100) recent ORDER BY id
                    """).param("orderId", orderId).query(this::map).list();
        }
        return db.sql("""
                SELECT * FROM order_messages WHERE order_id = :orderId AND id > :after ORDER BY id LIMIT 100
                """).param("orderId", orderId).param("after", after).query(this::map).list();
    }

    @Override
    public Message append(UUID orderId, UUID senderId, String message) {
        return db.sql("""
                INSERT INTO order_messages (order_id, sender_id, message) VALUES (:orderId, :senderId, :message)
                RETURNING *
                """).param("orderId", orderId).param("senderId", senderId).param("message", message)
                .query(this::map).single();
    }

    private Message map(ResultSet row, int index) throws SQLException {
        return new Message(row.getString("id"), row.getObject("sender_id", UUID.class),
                row.getString("message"), row.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
