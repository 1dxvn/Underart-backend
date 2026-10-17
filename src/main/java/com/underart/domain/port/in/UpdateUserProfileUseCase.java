package com.underart.domain.port.in;

import com.underart.domain.model.User;
import java.util.UUID;

public interface UpdateUserProfileUseCase {

    User update(UpdateProfileCommand cmd);

    record UpdateProfileCommand(UUID userId, String username, String city, String shippingAddress) {
    }
}
