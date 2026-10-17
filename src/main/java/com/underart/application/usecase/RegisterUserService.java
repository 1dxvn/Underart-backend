package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.RegisterUserUseCase;
import com.underart.domain.port.out.PasswordHasherPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    @Override
    public User register(RegisterUserCommand cmd) {
        String email = EmailNormalizer.normalize(cmd.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("El email ya está registrado");
        }
        User user = new User(UUID.randomUUID(), cmd.username().trim(), email,
                passwordHasher.hash(cmd.password()), cmd.city(), cmd.shippingAddress(),
                null, Instant.now());
        return userRepository.save(user);
    }
}
