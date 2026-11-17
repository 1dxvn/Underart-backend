package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AuthResponse;
import com.underart.application.dto.LoginRequest;
import com.underart.application.dto.RefreshTokenRequest;
import com.underart.domain.port.in.AuthSessionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "JWT login, refresh and logout")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthSessionUseCase authSessionUseCase;

    @PostMapping("/login")
    @Operation(summary = "Sign in and receive an access token and refresh token")
    @SecurityRequirements
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(AuthResponse.from(authSessionUseCase.login(req.email(), req.password())));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh a session and rotate the refresh token")
    @SecurityRequirements
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(AuthResponse.from(authSessionUseCase.refresh(req.refreshToken())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out and revoke the refresh token family")
    @SecurityRequirements
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest req) {
        authSessionUseCase.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
