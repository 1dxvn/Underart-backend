package com.underart.application.usecase;

import com.underart.domain.model.Order;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.ListOrdersUseCase;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListOrdersService implements ListOrdersUseCase {

    private final OrderRepositoryPort orderRepository;

    @Override
    public PageResult<Order> listPurchases(UUID userId, PageQuery page) {
        return orderRepository.findByWinnerId(userId, page);
    }

    @Override
    public PageResult<Order> listSales(UUID userId, PageQuery page) {
        return orderRepository.findBySellerId(userId, page);
    }
}
