package com.underart.domain.port.in;

import com.underart.domain.model.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand cmd);

    record RegisterUserCommand(
            String username,
            String email,
            String password,
            String city,
            String shippingAddress) {
    }
}
