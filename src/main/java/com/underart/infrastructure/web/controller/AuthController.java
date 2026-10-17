package com.underart.infrastructure.web.controller;

import com.underart.application.dto.LoginRequest;
import com.underart.application.dto.LoginResponse;
import com.underart.application.dto.UserResponse;
import com.underart.domain.model.User;
import com.underart.domain.port.in.AuthenticateUserUseCase;
import com.underart.infrastructure.security.JwtService;
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

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = authenticateUserUseCase.authenticate(req.email(), req.password());
        String token = jwtService.generateToken(user.email(), user.id());
        return ResponseEntity.ok(new LoginResponse(token, UserResponse.from(user)));
    }
}
