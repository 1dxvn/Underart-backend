package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.Bid;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.infrastructure.persistence.PageMapper;
import com.underart.infrastructure.persistence.jpa.mapper.BidMapper;
import com.underart.infrastructure.persistence.jpa.repository.BidJpaRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class BidRepositoryAdapter implements BidRepositoryPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "bidTime");

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
    public PageResult<Bid> findByAuctionId(UUID auctionId, PageQuery page) {
        return PageMapper.toResult(repository.findByAuctionId(auctionId,
                PageMapper.toPageable(page, NEWEST_FIRST)), BidMapper::toDomain);
    }

    @Override
    public PageResult<Bid> findByBuyerId(UUID buyerId, PageQuery page) {
        return PageMapper.toResult(repository.findByBuyerId(buyerId,
                PageMapper.toPageable(page, NEWEST_FIRST)), BidMapper::toDomain);
    }

    @Override
    public Optional<Bid> findHighestBid(UUID auctionId) {
        return repository.findFirstByAuctionIdOrderByAmountDescBidTimeAscIdAsc(auctionId).map(BidMapper::toDomain);
    }

    @Override
    public boolean existsByAuctionIdAndBuyerId(UUID auctionId, UUID buyerId) {
        return repository.existsByAuctionIdAndBuyerId(auctionId, buyerId);
    }

    @Override
    public long countByAuctionId(UUID auctionId) {
        return repository.countByAuctionId(auctionId);
    }

    @Override
    public Map<UUID, BidStats> statsByAuctionIds(Collection<UUID> auctionIds) {
        if (auctionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BidStats> stats = new HashMap<>();
        for (Object[] row : repository.statsByAuctionIds(auctionIds)) {
            stats.put((UUID) row[0], new BidStats((BigDecimal) row[1], ((Number) row[2]).longValue()));
        }
        return stats;
    }
}
