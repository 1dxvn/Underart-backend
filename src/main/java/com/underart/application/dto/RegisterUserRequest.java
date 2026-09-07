package com.underart.application.dto;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.User;

public record RegisterUserRequest(String username, String email, String password, User.Role role) {

    public RegisterUserRequest {
        if (password == null || password.length() < 8)
            throw new BusinessRuleException("La contraseña debe tener al menos 8 caracteres");
        if (role == null) role = User.Role.COLLECTOR;
    }
}
