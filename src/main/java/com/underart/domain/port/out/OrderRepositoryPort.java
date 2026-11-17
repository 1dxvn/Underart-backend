package com.underart.domain.port.out;

import com.underart.domain.model.Order;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdForUpdate(UUID id);

    boolean reservesArtwork(UUID artworkId);

    List<UUID> findUnpaidExpiredIds(Instant now);

    PageResult<Order> findByWinnerId(UUID winnerId, PageQuery page);

    PageResult<Order> findBySellerId(UUID sellerId, PageQuery page);

    boolean existsByAuctionId(UUID auctionId);

    long countByWinnerId(UUID winnerId);

    SalesStats salesStatsForSeller(UUID sellerId);

    record SalesStats(long count, BigDecimal paidRevenue) {
    }
}
