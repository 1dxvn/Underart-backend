package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.InvalidSessionException;
import com.underart.domain.exception.TooManyRequestsException;
import com.underart.domain.model.RefreshToken;
import com.underart.domain.model.User;
import com.underart.domain.port.in.AuthSessionUseCase;
import com.underart.domain.port.in.AuthenticateUserUseCase;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.AccessTokenPort.IssuedAccessToken;
import com.underart.domain.port.out.AccessTokenPort;
import com.underart.domain.port.out.RateLimiterPort;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessionService implements AuthSessionUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final FindUserUseCase findUserUseCase;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final AccessTokenPort accessTokens;
    private final RateLimiterPort rateLimiter;
    private final Duration refreshTokenTtl;

    public AuthSessionService(AuthenticateUserUseCase authenticateUserUseCase,
                              FindUserUseCase findUserUseCase,
                              RefreshTokenRepositoryPort refreshTokens,
                              AccessTokenPort accessTokens,
                              RateLimiterPort rateLimiter,
                              @Value("${auth.refresh-token-ttl:P30D}") Duration refreshTokenTtl) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.findUserUseCase = findUserUseCase;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.rateLimiter = rateLimiter;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    public Session login(String email, String password) {
        String key = "login:" + EmailNormalizer.normalize(email);
        rateLimiter.blockedFor(key, MAX_FAILED_LOGINS, LOGIN_WINDOW).ifPresent(wait -> {
            throw new TooManyRequestsException("Too many failed attempts. Try again in "
                    + ((wait.toSeconds() + 59) / 60) + " minutes", wait.toSeconds());
        });
        User user;
        try {
            user = authenticateUserUseCase.authenticate(email, password);
        } catch (BusinessRuleException e) {
            rateLimiter.hit(key, LOGIN_WINDOW);
            throw e;
        }
        rateLimiter.reset(key);
        return openSession(user, UUID.randomUUID()).session();
    }

    @Override
    @Transactional(noRollbackFor = InvalidSessionException.class)
    public Session refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokens.findByTokenHashForUpdate(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidSessionException("Invalid session. Sign in again"));
        Instant now = Instant.now();
        if (current.isRevoked()) {
            refreshTokens.revokeFamily(current.familyId(), now);
            throw new InvalidSessionException("Session revoked for security. Sign in again");
        }
        if (current.isExpired(now)) {
            throw new InvalidSessionException("Session expired. Sign in again");
        }
        OpenedSession next = openSession(findUserUseCase.findById(current.userId()), current.familyId());
        refreshTokens.save(current.revokedBy(next.tokenId(), now));
        return next.session();
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), Instant.now()));
    }

    private OpenedSession openSession(User user, UUID familyId) {
        String rawRefreshToken = newRawToken();
        Instant now = Instant.now();
        UUID tokenId = UUID.randomUUID();
        refreshTokens.save(new RefreshToken(tokenId, user.id(), hash(rawRefreshToken), familyId,
                now.plus(refreshTokenTtl), now, null, null));
        IssuedAccessToken access = accessTokens.issue(user);
        return new OpenedSession(new Session(user, access.token(), access.expiresInSeconds(), rawRefreshToken), tokenId);
    }

    private record OpenedSession(Session session, UUID tokenId) {
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
