package com.underart.application.usecase;

import com.underart.domain.model.RefreshToken;
import com.underart.domain.model.User;
import com.underart.domain.port.in.AuthSessionUseCase;
import com.underart.domain.port.in.AuthenticateUserUseCase;
import com.underart.domain.port.out.AccessTokenPort;
import com.underart.domain.port.out.AccessTokenPort.IssuedAccessToken;
import com.underart.domain.port.out.RefreshTokenRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService implements AuthSessionUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final AccessTokenPort accessTokens;
    private final Duration refreshTokenTtl;

    public AuthSessionService(AuthenticateUserUseCase authenticateUserUseCase,
                              RefreshTokenRepositoryPort refreshTokens,
                              AccessTokenPort accessTokens,
                              @Value("${auth.refresh-token-ttl:P30D}") Duration refreshTokenTtl) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    public Session login(String email, String password) {
        User user = authenticateUserUseCase.authenticate(email, password);
        return openSession(user, UUID.randomUUID());
    }

    Session openSession(User user, UUID familyId) {
        String rawRefreshToken = newRawToken();
        Instant now = Instant.now();
        refreshTokens.save(new RefreshToken(UUID.randomUUID(), user.id(), hash(rawRefreshToken), familyId,
                now.plus(refreshTokenTtl), now, null, null));
        IssuedAccessToken access = accessTokens.issue(user);
        return new Session(user, access.token(), access.expiresInSeconds(), rawRefreshToken);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
