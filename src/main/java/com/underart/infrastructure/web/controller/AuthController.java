package com.underart.infrastructure.web.controller;

import com.underart.application.dto.LoginRequest;
import com.underart.application.dto.LoginResponse;
import com.underart.application.dto.UserResponse;
import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.User;
import com.underart.domain.port.out.UserRepositoryPort;
import com.underart.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // TODO: mover a un FindUserByEmailUseCase cuando se agreguen más consultas por email.
    private final UserRepositoryPort userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessRuleException("Credenciales inválidas"));
        if (!passwordEncoder.matches(req.password(), user.passwordHash())) {
            throw new BusinessRuleException("Credenciales inválidas");
        }
        String token = jwtService.generateToken(req.email(), user.id());
        return ResponseEntity.ok(new LoginResponse(token, UserResponse.from(user)));
    }
}
