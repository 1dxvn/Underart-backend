package com.underart.application.usecase;

import com.underart.domain.model.User;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.in.UpdatePaymentMethodUseCase;
import com.underart.domain.port.out.PaymentGatewayPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdatePaymentMethodService implements UpdatePaymentMethodUseCase {

    private final FindUserUseCase findUserUseCase;
    private final UserRepositoryPort userRepository;
    private final PaymentGatewayPort paymentGateway;

    @Override
    public User updatePaymentToken(UUID userId, String paymentMethodRef) {
        User user = findUserUseCase.findById(userId);
        String token = paymentGateway.registerPaymentMethod(user.email(), paymentMethodRef.trim());
        return userRepository.save(new User(user.id(), user.username(), user.email(),
                user.passwordHash(), user.city(), user.shippingAddress(), token, user.createdAt()));
    }
}
