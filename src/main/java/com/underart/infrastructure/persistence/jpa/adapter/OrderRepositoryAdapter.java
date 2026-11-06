package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.Order;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.infrastructure.persistence.PageMapper;
import com.underart.infrastructure.persistence.jpa.mapper.OrderMapper;
import com.underart.infrastructure.persistence.jpa.repository.OrderJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final OrderJpaRepository repository;

    public OrderRepositoryAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        return OrderMapper.toDomain(repository.save(OrderMapper.toEntity(order)));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(OrderMapper::toDomain);
    }

    @Override
    public PageResult<Order> findByWinnerId(UUID winnerId, PageQuery page) {
        return PageMapper.toResult(repository.findByWinnerId(winnerId,
                PageMapper.toPageable(page, NEWEST_FIRST)), OrderMapper::toDomain);
    }

    @Override
    public PageResult<Order> findBySellerId(UUID sellerId, PageQuery page) {
        return PageMapper.toResult(repository.findByAuction_Artwork_Artist_Id(sellerId,
                PageMapper.toPageable(page, NEWEST_FIRST)), OrderMapper::toDomain);
    }

    @Override
    public boolean existsByAuctionId(UUID auctionId) {
        return repository.existsByAuctionId(auctionId);
    }

    @Override
    public long countByWinnerId(UUID winnerId) {
        return repository.countByWinnerId(winnerId);
    }

    @Override
    public SalesStats salesStatsForSeller(UUID sellerId) {
        Object[] row = repository.salesStatsForSeller(sellerId).get(0);
        BigDecimal revenue = row[1] instanceof BigDecimal value ? value : new BigDecimal(row[1].toString());
        return new SalesStats(((Number) row[0]).longValue(), revenue.setScale(2, RoundingMode.HALF_UP));
    }
}
