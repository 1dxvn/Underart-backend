package com.underart.infrastructure.persistence.mongo.adapter;

import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.infrastructure.persistence.mongo.document.ActivityLogDocument;
import com.underart.infrastructure.persistence.mongo.repository.ActivityLogMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityLogRepositoryAdapter implements ActivityLogRepositoryPort {

    private final ActivityLogMongoRepository mongo;

    public ActivityLogRepositoryAdapter(ActivityLogMongoRepository mongo) {
        this.mongo = mongo;
    }

    @Override
    public void log(String action, Long userId, String detail) {
        mongo.save(ActivityLogDocument.of(action, userId, detail));
    }
}
