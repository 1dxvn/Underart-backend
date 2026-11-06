package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.InvalidSessionException;
import com.underart.domain.model.RefreshToken;
import com.underart.domain.model.User;
import com.underart.domain.port.in.AuthSessionUseCase.Session;
import com.underart.domain.port.in.AuthenticateUserUseCase;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.AccessTokenPort;
import com.underart.domain.port.out.AccessTokenPort.IssuedAccessToken;
import com.underart.domain.port.out.RefreshTokenRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthSessionServiceTest {

    private final AuthenticateUserUseCase authenticate = mock(AuthenticateUserUseCase.class);
    private final FindUserUseCase users = mock(FindUserUseCase.class);
    private final RefreshTokenRepositoryPort tokens = mock(RefreshTokenRepositoryPort.class);
    private final AccessTokenPort access = mock(AccessTokenPort.class);
    private final AuthSessionService service =
            new AuthSessionService(authenticate, users, tokens, access, Duration.ofDays(30));
    private final User user = new User(UUID.randomUUID(), "ana", "ana@test.com", "hash", null, null, null, Instant.now());

    @BeforeEach
    void setUp() {
        when(access.issue(any(User.class))).thenReturn(new IssuedAccessToken("jwt-access", 900));
        when(tokens.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void loginIssuesAccessAndRefreshTokenStoringOnlyTheHash() {
        when(authenticate.authenticate("ana@test.com", "password123")).thenReturn(user);

        Session session = service.login("ana@test.com", "password123");

        assertThat(session.accessToken()).isEqualTo("jwt-access");
        assertThat(session.expiresInSeconds()).isEqualTo(900);
        assertThat(session.refreshToken()).hasSizeGreaterThanOrEqualTo(43);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).save(saved.capture());
        assertThat(saved.getValue().tokenHash())
                .isEqualTo(AuthSessionService.hash(session.refreshToken()))
                .isNotEqualTo(session.refreshToken());
        assertThat(saved.getValue().userId()).isEqualTo(user.id());
        assertThat(saved.getValue().expiresAt()).isAfter(Instant.now().plus(Duration.ofDays(29)));
    }

    @Test
    void eachLoginStartsANewTokenFamily() {
        when(authenticate.authenticate(any(), any())).thenReturn(user);
        service.login("ana@test.com", "password123");
        service.login("ana@test.com", "password123");

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).familyId()).isNotEqualTo(saved.getAllValues().get(1).familyId());
    }

    private RefreshToken stored(String raw, Instant expiresAt, Instant revokedAt) {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), user.id(), AuthSessionService.hash(raw),
                UUID.randomUUID(), expiresAt, Instant.now().minusSeconds(60), revokedAt, null);
        when(tokens.findByTokenHashForUpdate(AuthSessionService.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void refreshRotatesTheTokenWithinTheSameFamily() {
        RefreshToken old = stored("viejo", Instant.now().plusSeconds(3600), null);
        when(users.findById(user.id())).thenReturn(user);

        Session session = service.refresh("viejo");

        assertThat(session.refreshToken()).isNotEqualTo("viejo");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens, org.mockito.Mockito.times(2)).save(saved.capture());
        RefreshToken issued = saved.getAllValues().get(0);
        RefreshToken revoked = saved.getAllValues().get(1);
        assertThat(issued.familyId()).isEqualTo(old.familyId());
        assertThat(revoked.id()).isEqualTo(old.id());
        assertThat(revoked.isRevoked()).isTrue();
        assertThat(revoked.replacedBy()).isEqualTo(issued.id());
    }

    @Test
    void reusingARevokedTokenRevokesTheWholeFamily() {
        RefreshToken reused = stored("robado", Instant.now().plusSeconds(3600), Instant.now().minusSeconds(10));

        assertThatThrownBy(() -> service.refresh("robado")).isInstanceOf(InvalidSessionException.class);
        verify(tokens).revokeFamily(org.mockito.ArgumentMatchers.eq(reused.familyId()), any(Instant.class));
        verify(tokens, never()).save(any());
    }

    @Test
    void expiredOrUnknownTokensAreRejected() {
        stored("caducado", Instant.now().minusSeconds(1), null);
        when(tokens.findByTokenHashForUpdate(AuthSessionService.hash("inventado"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("caducado")).isInstanceOf(InvalidSessionException.class);
        assertThatThrownBy(() -> service.refresh("inventado")).isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void logoutRevokesTheFamilyAndIsIdempotent() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), user.id(), AuthSessionService.hash("activo"),
                UUID.randomUUID(), Instant.now().plusSeconds(3600), Instant.now(), null, null);
        when(tokens.findByTokenHash(AuthSessionService.hash("activo"))).thenReturn(Optional.of(token));
        when(tokens.findByTokenHash(AuthSessionService.hash("desconocido"))).thenReturn(Optional.empty());

        service.logout("activo");
        service.logout("desconocido");

        verify(tokens).revokeFamily(org.mockito.ArgumentMatchers.eq(token.familyId()), any(Instant.class));
    }
}
