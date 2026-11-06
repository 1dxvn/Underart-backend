package com.underart.infrastructure.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public Optional<UUID> currentUserIdIfPresent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getDetails() instanceof UUID userId ? Optional.of(userId) : Optional.empty();
    }

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof UUID userId)) {
            throw new AuthenticationCredentialsNotFoundException("Se requiere autenticación");
        }
        return userId;
    }
}
