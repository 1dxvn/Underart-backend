package com.underart.application.dto;

import com.underart.domain.exception.BusinessRuleException;

public record RegisterUserRequest(String username, String email, String password) {

    public RegisterUserRequest {
        if (password == null || password.length() < 8)
            throw new BusinessRuleException("La contraseña debe tener al menos 8 caracteres");
    }
}
