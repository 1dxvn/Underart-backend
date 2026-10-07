package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.Bid;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.infrastructure.persistence.jpa.mapper.BidMapper;
import com.underart.infrastructure.persistence.jpa.repository.BidJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BidRepositoryAdapter implements BidRepositoryPort {

    private final BidJpaRepository repository;

    public BidRepositoryAdapter(BidJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Bid save(Bid bid) {
        return BidMapper.toDomain(repository.save(BidMapper.toEntity(bid)));
    }

    @Override
    public List<Bid> findByAuctionId(UUID auctionId) {
        return repository.findByAuctionIdOrderByBidTimeDesc(auctionId).stream()
                .map(BidMapper::toDomain).toList();
    }

    @Override
    public Optional<Bid> findHighestBid(UUID auctionId) {
        return repository.findFirstByAuctionIdOrderByAmountDesc(auctionId).map(BidMapper::toDomain);
    }
}
