package com.underart.infrastructure.persistence.jpa.mapper;

import com.underart.domain.model.User;
import com.underart.infrastructure.persistence.jpa.entity.UserEntity;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(), e.getPasswordHash(),
                e.getCity(), e.getShippingAddress(), e.getPaymentToken(), e.getCreatedAt());
    }

    public static UserEntity toEntity(User u) {
        return UserEntity.builder()
                .id(u.id())
                .username(u.username())
                .email(u.email())
                .passwordHash(u.passwordHash())
                .city(u.city())
                .shippingAddress(u.shippingAddress())
                .paymentToken(u.paymentToken())
                .createdAt(u.createdAt())
                .build();
    }
}
