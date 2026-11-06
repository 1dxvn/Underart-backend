package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.model.RatingSummary;
import com.underart.domain.model.Review;
import com.underart.domain.port.out.ReviewRepositoryPort;
import com.underart.infrastructure.persistence.PageMapper;
import com.underart.infrastructure.persistence.jpa.entity.ReviewEntity;
import com.underart.infrastructure.persistence.jpa.repository.ReviewJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {

    private final ReviewJpaRepository repository;

    public ReviewRepositoryAdapter(ReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Review save(Review review) {
        return toDomain(repository.save(ReviewEntity.builder()
                .id(review.id())
                .orderId(review.orderId())
                .reviewerId(review.reviewerId())
                .sellerId(review.sellerId())
                .rating(review.rating())
                .comment(review.comment())
                .createdAt(review.createdAt())
                .build()));
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return repository.existsByOrderId(orderId);
    }

    @Override
    public PageResult<Review> findBySeller(UUID sellerId, PageQuery page) {
        return PageMapper.toResult(repository.findBySellerId(sellerId,
                PageMapper.toPageable(page, Sort.by(Sort.Direction.DESC, "createdAt"))), this::toDomain);
    }

    @Override
    public RatingSummary summaryForSeller(UUID sellerId) {
        List<Object[]> rows = repository.summaryForSeller(sellerId);
        if (rows.isEmpty()) {
            return RatingSummary.NONE;
        }
        Object[] row = rows.get(0);
        return RatingSummary.of(row[0] == null ? null : ((Number) row[0]).doubleValue(), ((Number) row[1]).longValue());
    }

    private Review toDomain(ReviewEntity e) {
        return new Review(e.getId(), e.getOrderId(), e.getReviewerId(), e.getSellerId(), e.getRating(),
                e.getComment(), e.getCreatedAt());
    }
}
