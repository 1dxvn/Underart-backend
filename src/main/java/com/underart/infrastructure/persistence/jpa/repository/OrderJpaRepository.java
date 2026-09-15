package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.OrderEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
}
