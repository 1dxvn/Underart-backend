package com.underart.application.usecase;

import com.underart.domain.model.User;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.in.UpdateUserProfileUseCase;
import com.underart.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final FindUserUseCase findUserUseCase;
    private final UserRepositoryPort userRepository;

    @Override
    public User update(UpdateProfileCommand cmd) {
        User user = findUserUseCase.findById(cmd.userId());
        User updated = new User(user.id(), cmd.username().trim(), user.email(), user.passwordHash(),
                cmd.city(), cmd.shippingAddress(), user.paymentToken(), user.createdAt());
        return userRepository.save(updated);
    }
}
