package com.underart.domain.port.in;

import com.underart.domain.model.User;

import java.util.Optional;

public interface FindUserUseCase {

    User findById(Long id);

    Optional<User> findByEmail(String email);
}
