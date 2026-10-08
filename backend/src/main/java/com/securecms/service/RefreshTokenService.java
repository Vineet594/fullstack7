package com.securecms.service;

import com.securecms.entity.RefreshToken;
import com.securecms.entity.User;
import com.securecms.exception.InvalidTokenException;
import com.securecms.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh tokens are random opaque strings (256 bit). Only their SHA-256 hash is stored.
 * They are single-use (rotation): using one revokes it and a new one is issued. If an already-used
 * token shows up again (possible theft), ALL tokens of that user are revoked.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenDays;

    @Transactional
    public String create(User user) {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Instant now = Instant.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plus(Duration.ofDays(refreshTokenDays)))
                .revoked(false)
                .build());
        return rawToken;
    }

    /** Validates a refresh token, marks it as used and returns its owner. */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public User consume(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Refresh token is not valid"));
        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(token.getUser().getId());
            throw new InvalidTokenException("Refresh token has already been used or revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }
        token.setRevoked(true);
        return token.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(t -> t.setRevoked(true));
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
