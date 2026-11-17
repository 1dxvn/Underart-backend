package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.UpdatePaymentMethodUseCase;
import com.underart.domain.port.out.PaymentGatewayPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePaymentMethodService implements UpdatePaymentMethodUseCase {

    private final UserRepositoryPort userRepository;
    private final PaymentGatewayPort paymentGateway;

    @Override
    @Transactional
    public User updatePaymentToken(UUID userId, String paymentMethodRef) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String token = paymentGateway.registerPaymentMethod(user.email(), paymentMethodRef.trim());
        return userRepository.save(new User(user.id(), user.username(), user.email(),
                user.passwordHash(), user.city(), user.shippingAddress(), token, user.createdAt()));
    }
}
