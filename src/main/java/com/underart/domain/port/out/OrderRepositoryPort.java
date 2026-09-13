package com.underart.domain.port.out;

import com.underart.domain.model.Order;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(UUID id);
}
