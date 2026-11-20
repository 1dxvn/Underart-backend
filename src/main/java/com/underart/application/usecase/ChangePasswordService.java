package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.PasswordPolicy;
import com.underart.domain.model.User;
import com.underart.domain.port.in.ChangePasswordUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.PasswordHasherPort;
import com.underart.domain.port.out.RefreshTokenRepositoryPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordService implements ChangePasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final ActivityLogRepositoryPort activityLogRepository;
    private final RefreshTokenRepositoryPort refreshTokens;

    @Override
    @Transactional
    public void change(UUID userId, String currentPassword, String newPassword) {
        PasswordPolicy.require(newPassword);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
            throw new BusinessRuleException("The current password is incorrect");
        }
        if (currentPassword.equals(newPassword)) {
            throw new BusinessRuleException("The new password must differ from the current password");
        }
        userRepository.save(new User(user.id(), user.username(), user.email(),
                passwordHasher.hash(newPassword), user.city(), user.shippingAddress(),
                user.paymentToken(), user.createdAt(), user.bio(), user.avatarUrl(), user.avatarKey()));
        refreshTokens.revokeAllForUser(userId, Instant.now());
        activityLogRepository.log("PASSWORD_CHANGED", "Password updated; refresh sessions revoked", userId, Map.of());
    }
}
