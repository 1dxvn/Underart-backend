package com.underart.infrastructure.web.controller;

import com.underart.application.dto.PageResponse;
import com.underart.application.dto.PublicUserResponse;
import com.underart.application.dto.RegisterUserRequest;
import com.underart.application.dto.ReviewResponse;
import com.underart.application.dto.UserResponse;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.User;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.underart.domain.port.in.RegisterUserUseCase;
import com.underart.domain.port.in.ReviewsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Users", description = "Registration and public profiles")
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserUseCase registerUseCase;
    private final FindUserUseCase findUserUseCase;
    private final ReviewsUseCase reviewsUseCase;

    @PostMapping("/register")
    @Operation(summary = "Register a user")
    @SecurityRequirements
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest req) {
        User user = registerUseCase.register(new RegisterUserCommand(
                req.username(), req.email(), req.password(), req.city(), req.shippingAddress()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Public profile with average rating")
    @SecurityRequirements
    public ResponseEntity<PublicUserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(PublicUserResponse.from(findUserUseCase.findById(id), reviewsUseCase.ratingOf(id)));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Seller reviews")
    @SecurityRequirements
    public ResponseEntity<PageResponse<ReviewResponse>> reviews(@PathVariable UUID id,
                                                                @RequestParam(required = false) Integer page,
                                                                @RequestParam(required = false) Integer size) {
        findUserUseCase.findById(id);
        return ResponseEntity.ok(PageResponse.from(reviewsUseCase.listForSeller(id, PageQuery.of(page, size)),
                ReviewResponse::from));
    }
}
