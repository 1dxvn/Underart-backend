package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.AuthenticateUserUseCase;
import com.underart.domain.port.out.PasswordHasherPort;
import com.underart.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private static final String INVALID = "Credenciales inválidas";

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    @Override
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new BusinessRuleException(INVALID));
        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw new BusinessRuleException(INVALID);
        }
        return user;
    }
}
