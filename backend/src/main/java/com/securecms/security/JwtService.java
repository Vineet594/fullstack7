package com.securecms.security;

import com.securecms.entity.User;
import com.securecms.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Creates and validates short-lived JWT access tokens (HS256). Tokens are never logged. */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-expiration-seconds}") long accessTokenTtlSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().getName().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenTtlSeconds * 1000))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            Number userId = (Number) claims.get("userId");
            String role = claims.get("role", String.class);
            if (userId == null || role == null || claims.getSubject() == null) {
                throw new InvalidTokenException("Token is missing required claims");
            }
            return new AuthenticatedUser(userId.longValue(), claims.getSubject(), role);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Token is invalid or has expired");
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}
