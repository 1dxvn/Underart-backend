package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.ChangePasswordUseCase;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.PasswordHasherPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordService implements ChangePasswordUseCase {

    private final FindUserUseCase findUserUseCase;
    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public void change(UUID userId, String currentPassword, String newPassword) {
        User user = findUserUseCase.findById(userId);
        if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
            throw new BusinessRuleException("La contraseña actual no es correcta");
        }
        if (currentPassword.equals(newPassword)) {
            throw new BusinessRuleException("La nueva contraseña debe ser distinta de la actual");
        }
        userRepository.save(new User(user.id(), user.username(), user.email(),
                passwordHasher.hash(newPassword), user.city(), user.shippingAddress(),
                user.paymentToken(), user.createdAt()));
        activityLogRepository.log("PASSWORD_CHANGED", "Contraseña actualizada", userId, Map.of());
    }
}
