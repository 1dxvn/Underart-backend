package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.RegisterUserUseCase;
import com.underart.domain.port.out.UserRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterUserCommand cmd) {
        if (userRepository.existsByEmail(cmd.email())) {
            throw new BusinessRuleException("El email ya está registrado");
        }
        User user = new User(UUID.randomUUID(), cmd.username(), cmd.email(),
                passwordEncoder.encode(cmd.password()), cmd.city(), cmd.shippingAddress(),
                null, Instant.now());
        return userRepository.save(user);
    }
}
