package com.securecms.security;

import com.securecms.entity.User;
import com.securecms.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and validates short-lived JWT ACCESS tokens (signed with HMAC-SHA).
 * Claims: sub = username, userId, role, iat, exp.
 * Tokens are never logged.
 */
@Component
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-expiration-seconds}") long accessTokenTtlSeconds) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 characters long. Generate one with: openssl rand -base64 48");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().getName().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(signingKey)
                .compact();
    }

    /** Validates signature + expiry and returns the identity stored in the token. */
    public AuthenticatedUser parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Number userId = claims.get(CLAIM_USER_ID, Number.class);
            String role = claims.get(CLAIM_ROLE, String.class);
            if (userId == null || role == null || claims.getSubject() == null) {
                throw new InvalidTokenException("INVALID_TOKEN", "Access token is invalid");
            }
            return new AuthenticatedUser(userId.longValue(), claims.getSubject(), role);
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("TOKEN_EXPIRED", "Access token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("INVALID_TOKEN", "Access token is invalid");
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}
