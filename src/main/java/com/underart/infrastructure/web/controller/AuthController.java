package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AuthResponse;
import com.underart.application.dto.LoginRequest;
import com.underart.application.dto.RefreshTokenRequest;
import com.underart.domain.port.in.AuthSessionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthSessionUseCase authSessionUseCase;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(AuthResponse.from(authSessionUseCase.login(req.email(), req.password())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(AuthResponse.from(authSessionUseCase.refresh(req.refreshToken())));
    }
}
