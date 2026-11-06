package com.underart.application.dto;

import com.underart.domain.model.RatingSummary;
import com.underart.domain.model.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PublicUserResponse(
        UUID id,
        String username,
        String city,
        Instant createdAt,
        BigDecimal averageRating,
        long reviewsCount) {

    public static PublicUserResponse from(User user, RatingSummary rating) {
        return new PublicUserResponse(user.id(), user.username(), user.city(), user.createdAt(), rating.average(),
                rating.count());
    }
}
