package com.underart.domain.port.out;

import com.underart.domain.model.Order;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    PageResult<Order> findByWinnerId(UUID winnerId, PageQuery page);

    PageResult<Order> findBySellerId(UUID sellerId, PageQuery page);

    boolean existsByAuctionId(UUID auctionId);
}
