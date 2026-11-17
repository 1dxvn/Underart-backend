package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.out.FavoriteRepositoryPort;
import com.underart.infrastructure.persistence.PageMapper;
import com.underart.infrastructure.persistence.jpa.entity.FavoriteEntity;
import com.underart.infrastructure.persistence.jpa.entity.FavoriteId;
import com.underart.infrastructure.persistence.jpa.repository.FavoriteJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class FavoriteRepositoryAdapter implements FavoriteRepositoryPort {

    private final FavoriteJpaRepository repository;

    public FavoriteRepositoryAdapter(FavoriteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void add(UUID userId, UUID artworkId) {
        FavoriteId id = new FavoriteId(userId, artworkId);
        if (repository.existsById(id)) {
            return;
        }
        try {
            repository.save(new FavoriteEntity(id, Instant.now()));
        } catch (DataIntegrityViolationException alreadyAddedConcurrently) {
        }
    }

    @Override
    public void remove(UUID userId, UUID artworkId) {
        repository.deleteById(new FavoriteId(userId, artworkId));
    }

    @Override
    public boolean exists(UUID userId, UUID artworkId) {
        return repository.existsById(new FavoriteId(userId, artworkId));
    }

    @Override
    public long countByArtwork(UUID artworkId) {
        return repository.countByIdArtworkId(artworkId);
    }

    @Override
    public PageResult<UUID> artworkIdsByUser(UUID userId, PageQuery page) {
        return PageMapper.toResult(repository.findByIdUserId(userId,
                PageMapper.toPageable(page, Sort.by(Sort.Direction.DESC, "createdAt"))), f -> f.getId().getArtworkId());
    }
}
