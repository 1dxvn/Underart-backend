package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.UpdateUserProfileUseCase;
import com.underart.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final UserRepositoryPort userRepository;

    @Override
    @Transactional
    public User update(UpdateProfileCommand cmd) {
        User user = userRepository.findByIdForUpdate(cmd.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User updated = new User(user.id(), cmd.username().trim(), user.email(), user.passwordHash(),
                cmd.city(), cmd.shippingAddress(), user.paymentToken(), user.createdAt(),
                cmd.bio() == null ? user.bio() : cmd.bio().strip(), user.avatarUrl(), user.avatarKey());
        return userRepository.save(updated);
    }
}
