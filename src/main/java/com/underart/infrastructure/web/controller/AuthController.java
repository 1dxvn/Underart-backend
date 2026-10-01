package com.underart.infrastructure.web.controller;

import com.underart.application.dto.LoginRequest;
import com.underart.application.dto.LoginResponse;
import com.underart.application.dto.UserResponse;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.User;
import com.underart.domain.port.out.UserRepositoryPort;
import com.underart.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    // TODO: mover a un FindUserByEmailUseCase cuando se agreguen más consultas por email.
    private final UserRepositoryPort userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + req.email()));
        String token = jwtService.generateToken(req.email(), user.id());
        return ResponseEntity.ok(new LoginResponse(token, UserResponse.from(user)));
    }
}
