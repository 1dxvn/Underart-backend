package com.underart.application.dto;

import com.underart.domain.model.Review;
import com.underart.domain.model.ReviewView;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderId,
        UUID reviewerId,
        String reviewerUsername,
        UUID sellerId,
        int rating,
        String comment,
        Instant createdAt) {

    public static ReviewResponse from(ReviewView view) {
        Review r = view.review();
        return new ReviewResponse(r.id(), r.orderId(), r.reviewerId(), view.reviewerUsername(), r.sellerId(),
                r.rating(), r.comment(), r.createdAt());
    }
}
