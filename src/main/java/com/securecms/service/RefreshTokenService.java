package com.securecms.service;

import com.securecms.entity.RefreshToken;
import com.securecms.entity.User;
import com.securecms.repository.RefreshTokenRepository;
import com.securecms.util.TokenHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Refresh tokens are random opaque strings (256 bits). Only their SHA-256 hash is stored in the database.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh-token-expiration-days}") long refreshTokenDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenDays);
    }

    /** Creates a new refresh token and returns the RAW value (shown to the client only once). */
    @Transactional
    public String issue(User user) {
        String rawToken = TokenHasher.generateRandomToken();

        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(TokenHasher.sha256(rawToken));
        entity.setUser(user);
        entity.setExpiresAt(Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    /** Housekeeping: removes expired refresh tokens every night at 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        int removed = refreshTokenRepository.deleteExpired(Instant.now());
        log.info("Removed {} expired refresh tokens", removed);
    }
}
