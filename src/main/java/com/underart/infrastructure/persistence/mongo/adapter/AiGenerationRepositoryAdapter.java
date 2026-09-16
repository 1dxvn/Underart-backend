package com.underart.infrastructure.persistence.mongo.adapter;

import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.infrastructure.persistence.mongo.document.AiGenerationDocument;
import com.underart.infrastructure.persistence.mongo.repository.AiGenerationMongoRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AiGenerationRepositoryAdapter implements AiGenerationRepositoryPort {

    private final AiGenerationMongoRepository repository;

    public AiGenerationRepositoryAdapter(AiGenerationMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AiGenerationRecord record) {
        repository.save(new AiGenerationDocument(null, record.provider(), record.artworkTitle(),
                record.suggestedPrice(), record.tags(), record.rawResponse(), Instant.now()));
    }
}
