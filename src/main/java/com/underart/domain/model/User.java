package com.underart.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String username,
        String email,
        String passwordHash,
        String city,
        String shippingAddress,
        String paymentToken,
        Instant createdAt,
        String bio,
        String avatarUrl,
        String avatarKey) {

    public User(UUID id, String username, String email, String passwordHash, String city,
            String shippingAddress, String paymentToken, Instant createdAt) {
        this(id, username, email, passwordHash, city, shippingAddress, paymentToken, createdAt, null, null, null);
    }

    public User withAvatar(String url, String key) {
        return new User(id, username, email, passwordHash, city, shippingAddress, paymentToken,
                createdAt, bio, url, key);
    }
}
