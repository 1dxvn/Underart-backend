package com.underart.domain.port.in;

import java.util.UUID;

public interface ChangePasswordUseCase {

    void change(UUID userId, String currentPassword, String newPassword);
}
