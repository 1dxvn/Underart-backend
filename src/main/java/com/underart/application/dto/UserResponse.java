package com.underart.application.dto;

import com.underart.domain.model.User;
import java.util.UUID;

public record UserResponse(UUID id, String username, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.email());
    }
}
