package com.underart.domain.port.in;

import com.underart.domain.model.User;
import java.util.UUID;

public interface UpdateUserProfileUseCase {

    User update(UpdateProfileCommand cmd);

    record UpdateProfileCommand(UUID userId, String username, String city, String shippingAddress, String bio) {
        public UpdateProfileCommand(UUID userId, String username, String city, String shippingAddress) {
            this(userId, username, city, shippingAddress, null);
        }
    }
}
