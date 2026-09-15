package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.User;
import com.underart.domain.port.out.UserRepositoryPort;
import com.underart.infrastructure.persistence.jpa.mapper.UserMapper;
import com.underart.infrastructure.persistence.jpa.repository.UserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    public UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return UserMapper.toDomain(repository.save(UserMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
