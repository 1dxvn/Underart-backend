package com.underart.domain.port.out;

import java.util.Map;
import java.util.UUID;

public interface ActivityLogRepositoryPort {

    void log(String eventType, String description, UUID userId, Map<String, Object> metadata);
}
