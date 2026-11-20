package com.underart.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderMessageRepository {
    List<Message> list(UUID orderId, Long after);
    Message append(UUID orderId, UUID senderId, String message);

    record Message(String id, UUID senderId, String message, Instant createdAt) {}
}
