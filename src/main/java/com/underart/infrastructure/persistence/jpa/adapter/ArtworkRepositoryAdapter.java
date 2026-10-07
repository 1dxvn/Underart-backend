package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.Artwork;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.infrastructure.persistence.jpa.mapper.ArtworkMapper;
import com.underart.infrastructure.persistence.jpa.repository.ArtworkJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ArtworkRepositoryAdapter implements ArtworkRepositoryPort {

    private final ArtworkJpaRepository repository;

    public ArtworkRepositoryAdapter(ArtworkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Artwork save(Artwork artwork) {
        return ArtworkMapper.toDomain(repository.save(ArtworkMapper.toEntity(artwork)));
    }

    @Override
    public Optional<Artwork> findById(UUID id) {
        return repository.findById(id).map(ArtworkMapper::toDomain);
    }

    @Override
    public List<Artwork> findByArtistId(UUID artistId) {
        return repository.findByArtistId(artistId).stream().map(ArtworkMapper::toDomain).toList();
    }

    @Override
    public List<Artwork> findAll() {
        return repository.findAll().stream().map(ArtworkMapper::toDomain).toList();
    }
}
