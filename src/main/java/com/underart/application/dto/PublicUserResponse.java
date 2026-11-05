package com.underart.application.dto;

import com.underart.domain.model.User;
import java.time.Instant;
import java.util.UUID;

public record PublicUserResponse(UUID id, String username, String city, Instant createdAt) {

    public static PublicUserResponse from(User user) {
        return new PublicUserResponse(user.id(), user.username(), user.city(), user.createdAt());
    }
}
