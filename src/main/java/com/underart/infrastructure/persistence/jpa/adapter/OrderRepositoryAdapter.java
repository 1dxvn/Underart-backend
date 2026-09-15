package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.Order;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.infrastructure.persistence.jpa.mapper.OrderMapper;
import com.underart.infrastructure.persistence.jpa.repository.OrderJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

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
}
