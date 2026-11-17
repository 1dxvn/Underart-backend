package com.underart.domain.port.out;

import com.underart.domain.model.User;

public interface AccessTokenPort {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String token, long expiresInSeconds) {
    }
}
