package com.underart.domain.port.in;

import com.underart.domain.model.User;

public interface RegisterUserUseCase {

    User register(String username, String email, String passwordHash);
}
