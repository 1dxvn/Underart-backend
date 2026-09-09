package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.RegisterUserUseCase;
import com.underart.domain.port.out.UserRepositoryPort;
import java.time.Instant;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort users;

    public RegisterUserService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public User register(String username, String email, String passwordHash) {
        if (users.existsByEmail(email)) throw new BusinessRuleException("El email ya está registrado");
        return users.save(new User(null, username, email, passwordHash, null, null, null, Instant.now()));
    }
}
