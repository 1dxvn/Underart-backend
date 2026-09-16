package com.underart.infrastructure.persistence.mongo.repository;

import com.underart.infrastructure.persistence.mongo.document.AiGenerationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiGenerationMongoRepository extends MongoRepository<AiGenerationDocument, String> {
}
