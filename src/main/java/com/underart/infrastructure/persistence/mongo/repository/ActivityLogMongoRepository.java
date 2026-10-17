package com.underart.infrastructure.persistence.mongo.repository;

import com.underart.infrastructure.persistence.mongo.document.ActivityLogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityLogMongoRepository extends MongoRepository<ActivityLogDocument, String> {

    Page<ActivityLogDocument> findByUserId(String userId, Pageable pageable);
}
