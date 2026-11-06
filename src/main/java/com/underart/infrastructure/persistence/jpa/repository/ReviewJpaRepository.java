package com.underart.infrastructure.persistence.jpa.repository;

import com.underart.infrastructure.persistence.jpa.entity.ReviewEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

    boolean existsByOrderId(UUID orderId);

    Page<ReviewEntity> findBySellerId(UUID sellerId, Pageable pageable);

    @Query("select avg(r.rating), count(r) from ReviewEntity r where r.sellerId = :seller")
    List<Object[]> summaryForSeller(@Param("seller") UUID sellerId);
}
