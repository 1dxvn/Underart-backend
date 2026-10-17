package com.underart.infrastructure.persistence.mongo.adapter;

import com.underart.domain.model.ActivityLog;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.infrastructure.persistence.PageMapper;
import com.underart.infrastructure.persistence.mongo.document.ActivityLogDocument;
import com.underart.infrastructure.persistence.mongo.repository.ActivityLogMongoRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogRepositoryAdapter implements ActivityLogRepositoryPort {

    private final ActivityLogMongoRepository repository;

    public ActivityLogRepositoryAdapter(ActivityLogMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(String eventType, String description, UUID userId, Map<String, Object> metadata) {
        repository.save(new ActivityLogDocument(null, eventType, description,
                userId != null ? userId.toString() : null, metadata, Instant.now()));
    }

    @Override
    public PageResult<ActivityLog> findByUserId(UUID userId, PageQuery page) {
        return PageMapper.toResult(repository.findByUserId(userId.toString(),
                PageMapper.toPageable(page, Sort.by(Sort.Direction.DESC, "createdAt"))), this::toDomain);
    }

    private ActivityLog toDomain(ActivityLogDocument d) {
        return new ActivityLog(d.id(), d.eventType(), d.description(),
                d.userId() == null ? null : UUID.fromString(d.userId()), d.metadata(), d.createdAt());
    }
}
