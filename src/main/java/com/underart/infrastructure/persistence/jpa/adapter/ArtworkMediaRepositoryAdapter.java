package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.infrastructure.persistence.jpa.mapper.ArtworkMediaMapper;
import com.underart.infrastructure.persistence.jpa.repository.ArtworkMediaJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ArtworkMediaRepositoryAdapter implements ArtworkMediaRepositoryPort {

    private final ArtworkMediaJpaRepository repository;

    public ArtworkMediaRepositoryAdapter(ArtworkMediaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ArtworkMedia save(ArtworkMedia media) {
        return ArtworkMediaMapper.toDomain(repository.save(ArtworkMediaMapper.toEntity(media)));
    }

    @Override
    public List<ArtworkMedia> saveAll(List<ArtworkMedia> media) {
        return repository.saveAll(media.stream().map(ArtworkMediaMapper::toEntity).toList()).stream()
                .map(ArtworkMediaMapper::toDomain).toList();
    }

    @Override
    public Optional<ArtworkMedia> findById(UUID id) {
        return repository.findById(id).map(ArtworkMediaMapper::toDomain);
    }

    @Override
    public List<ArtworkMedia> findByArtworkId(UUID artworkId) {
        return repository.findByArtworkIdOrderByPositionAsc(artworkId).stream()
                .map(ArtworkMediaMapper::toDomain).toList();
    }

    @Override
    public long countByArtworkId(UUID artworkId) {
        return repository.countByArtworkId(artworkId);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
