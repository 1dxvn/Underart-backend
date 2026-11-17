package com.underart.application.dto;

import com.underart.domain.port.in.AuthSessionUseCase.Session;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user) {

    public static AuthResponse from(Session session) {
        return new AuthResponse(session.accessToken(), session.refreshToken(), "Bearer",
                session.expiresInSeconds(), UserResponse.from(session.user()));
    }
}
