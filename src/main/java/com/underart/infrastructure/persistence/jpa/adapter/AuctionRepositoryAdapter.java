package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.infrastructure.persistence.jpa.mapper.AuctionMapper;
import com.underart.infrastructure.persistence.jpa.repository.AuctionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuctionRepositoryAdapter implements AuctionRepositoryPort {

    private final AuctionJpaRepository repository;

    public AuctionRepositoryAdapter(AuctionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Auction save(Auction auction) {
        return AuctionMapper.toDomain(repository.save(AuctionMapper.toEntity(auction)));
    }

    @Override
    public Optional<Auction> findById(UUID id) {
        return repository.findById(id).map(AuctionMapper::toDomain);
    }

    @Override
    public List<Auction> findByStatus(AuctionStatus status) {
        return repository.findByStatus(status).stream().map(AuctionMapper::toDomain).toList();
    }

    @Override
    public List<Auction> findAll() {
        return repository.findAll().stream().map(AuctionMapper::toDomain).toList();
    }
}
