package com.underart.domain.port.in;

import com.underart.domain.model.User;

public interface AuthenticateUserUseCase {

    User authenticate(String email, String password);
}
