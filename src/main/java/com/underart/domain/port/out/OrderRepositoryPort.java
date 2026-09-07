package com.underart.domain.port.out;

import com.underart.domain.model.Order;

import java.util.List;

public interface OrderRepositoryPort {

    Order save(Order order);

    List<Order> findByBuyerId(Long buyerId);
}
