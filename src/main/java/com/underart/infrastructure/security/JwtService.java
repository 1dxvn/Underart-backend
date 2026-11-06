package com.underart.infrastructure.security;

import com.underart.domain.model.User;
import com.underart.domain.port.out.AccessTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService implements AccessTokenPort {

    static final String ISSUER = "underart-backend";
    static final String AUDIENCE = "underart-api";
    private static final int MIN_SECRET_BYTES = 32;
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String USER_ID_CLAIM = "userId";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms:900000}") long expirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES + " caracteres");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        return new IssuedAccessToken(generateToken(user.email(), user.id()), expirationSeconds());
    }

    public long expirationSeconds() {
        return expirationMs / 1000;
    }

    public String generateToken(String email, UUID userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(email)
                .claim(USER_ID_CLAIM, userId.toString())
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).get(USER_ID_CLAIM, String.class));
    }

    private Claims extractAllClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!ACCESS_TYPE.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("El token no es de acceso");
        }
        return claims;
    }
}
