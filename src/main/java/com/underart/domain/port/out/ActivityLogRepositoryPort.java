package com.underart.domain.port.out;

import com.underart.domain.model.ActivityLog;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.Map;
import java.util.UUID;

public interface ActivityLogRepositoryPort {

    void log(String eventType, String description, UUID userId, Map<String, Object> metadata);

    PageResult<ActivityLog> findByUserId(UUID userId, PageQuery page);
}
