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
        Instant createdAt) {
}
