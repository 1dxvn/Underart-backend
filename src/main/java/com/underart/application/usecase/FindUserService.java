package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.UserRepositoryPort;

import java.util.Optional;

public class FindUserService implements FindUserUseCase {

    private final UserRepositoryPort users;

    public FindUserService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public User findById(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email);
    }
}
