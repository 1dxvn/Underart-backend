package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.model.RefreshToken;
import com.underart.domain.model.User;
import com.underart.domain.port.in.AuthSessionUseCase.Session;
import com.underart.domain.port.in.AuthenticateUserUseCase;
import com.underart.domain.port.out.AccessTokenPort;
import com.underart.domain.port.out.AccessTokenPort.IssuedAccessToken;
import com.underart.domain.port.out.RefreshTokenRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthSessionServiceTest {

    private final AuthenticateUserUseCase authenticate = mock(AuthenticateUserUseCase.class);
    private final RefreshTokenRepositoryPort tokens = mock(RefreshTokenRepositoryPort.class);
    private final AccessTokenPort access = mock(AccessTokenPort.class);
    private final AuthSessionService service =
            new AuthSessionService(authenticate, tokens, access, Duration.ofDays(30));
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
}
