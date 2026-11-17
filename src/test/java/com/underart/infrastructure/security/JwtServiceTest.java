package com.underart.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-more-than-32-bytes-0123456789";
    private final JwtService jwt = new JwtService(SECRET, 60_000);

    @Test
    void generatedTokenIsValidAndCarriesTheUser() {
        UUID userId = UUID.randomUUID();
        String token = jwt.generateToken("ana@test.com", userId);

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.extractEmail(token)).isEqualTo("ana@test.com");
        assertThat(jwt.extractUserId(token)).isEqualTo(userId);
        assertThat(jwt.expirationSeconds()).isEqualTo(60);
    }

    @Test
    void rejectsTamperedExpiredOrForeignTokens() {
        String token = jwt.generateToken("ana@test.com", UUID.randomUUID());
        JwtService otherKey = new JwtService("another-secret-key-with-more-than-32-bytes-xyz", 60_000);
        JwtService expired = new JwtService(SECRET, -60_000);

        String[] parts = token.split("\\.");
        String otherPayload = jwt.generateToken("eve@test.com", UUID.randomUUID()).split("\\.")[1];
        String tampered = parts[0] + "." + otherPayload + "." + parts[2];

        assertThat(jwt.validateToken(tampered)).isFalse();
        assertThat(otherKey.validateToken(token)).isFalse();
        assertThat(jwt.validateToken(expired.generateToken("ana@test.com", UUID.randomUUID()))).isFalse();
        assertThat(jwt.validateToken("no-es-un-jwt")).isFalse();
    }

    @Test
    void rejectsTokensWithWrongIssuerAudienceOrType() {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date exp = new Date(System.currentTimeMillis() + 60_000);
        String foreignIssuer = Jwts.builder().issuer("otro").audience().add(JwtService.AUDIENCE).and()
                .claim("type", "access").claim("userId", UUID.randomUUID().toString()).expiration(exp)
                .signWith(key).compact();
        String noType = Jwts.builder().issuer(JwtService.ISSUER).audience().add(JwtService.AUDIENCE).and()
                .claim("userId", UUID.randomUUID().toString()).expiration(exp).signWith(key).compact();
        String refreshLike = Jwts.builder().issuer(JwtService.ISSUER).audience().add(JwtService.AUDIENCE).and()
                .claim("type", "refresh").expiration(exp).signWith(key).compact();

        assertThat(jwt.validateToken(foreignIssuer)).isFalse();
        assertThat(jwt.validateToken(noType)).isFalse();
        assertThat(jwt.validateToken(refreshLike)).isFalse();
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> new JwtService("corto", 60_000)).isInstanceOf(IllegalStateException.class);
    }
}
