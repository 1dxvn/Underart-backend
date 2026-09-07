package com.underart.application.dto;

import com.underart.domain.model.User;

public record UserResponse(Long id, String username, String email, User.Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.email(), user.role());
    }
}
