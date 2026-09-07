package com.underart.infrastructure.persistence.mongo.adapter;

import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.infrastructure.persistence.mongo.document.AiGenerationDocument;
import com.underart.infrastructure.persistence.mongo.repository.AiGenerationMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AiGenerationRepositoryAdapter implements AiGenerationRepositoryPort {

    private final AiGenerationMongoRepository mongo;

    public AiGenerationRepositoryAdapter(AiGenerationMongoRepository mongo) {
        this.mongo = mongo;
    }

    @Override
    public void save(String provider, String prompt, String output, long latencyMs) {
        mongo.save(AiGenerationDocument.of(provider, prompt, output, latencyMs));
    }
}
