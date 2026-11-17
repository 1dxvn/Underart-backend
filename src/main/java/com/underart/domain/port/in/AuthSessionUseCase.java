package com.underart.domain.port.in;

import com.underart.domain.model.User;

public interface AuthSessionUseCase {

    Session login(String email, String password);

    Session refresh(String refreshToken);

    void logout(String refreshToken);

    record Session(User user, String accessToken, long expiresInSeconds, String refreshToken) {
    }
}
