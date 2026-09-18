package com.underart.application.dto;

public record LoginResponse(String token, UserResponse user) {
}
