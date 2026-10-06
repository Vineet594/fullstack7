package com.securecms.service;

import com.securecms.dto.LoginRequest;
import com.securecms.dto.LoginResponse;
import com.securecms.dto.RegisterRequest;
import com.securecms.dto.UserResponse;
import com.securecms.entity.RefreshToken;
import com.securecms.entity.RoleName;
import com.securecms.entity.User;
import com.securecms.exception.InvalidTokenException;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.repository.RefreshTokenRepository;
import com.securecms.repository.UserRepository;
import com.securecms.security.JwtService;
import com.securecms.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Token lifecycle:
 *  login   -> access token (15 min, JWT) + refresh token (7 days, opaque, stored hashed)
 *  refresh -> old refresh token is revoked (rotation) and a new access + refresh token pair is issued
 *  logout  -> refresh token is revoked
 * If an already-revoked refresh token is presented again (possible theft), ALL sessions of that user are revoked.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    /** Public registration always creates a normal USER - the role cannot be chosen by the caller. */
    public UserResponse register(RegisterRequest request) {
        return userService.createUser(request.username(), request.email(), request.password(),
                request.phone(), RoleName.USER);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Throws BadCredentialsException (-> 401) if username or password is wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.username()));

        log.info("User '{}' logged in successfully", user.getUsername());
        return buildTokens(user);
    }

    // noRollbackFor: the "revoke everything" safety action must be committed even though we throw afterwards
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public LoginResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .orElseThrow(() -> new InvalidTokenException("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            log.warn("Revoked refresh token was reused - all sessions revoked for userId={}", stored.getUser().getId());
            throw new InvalidTokenException("REFRESH_TOKEN_REVOKED",
                    "Refresh token has been revoked. Please log in again");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("REFRESH_TOKEN_EXPIRED",
                    "Refresh token has expired. Please log in again");
        }

        stored.setRevoked(true); // rotation: every refresh token can be used only once
        return buildTokens(stored.getUser());
    }

    /** Idempotent: unknown / already revoked tokens are silently ignored. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private LoginResponse buildTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(accessToken, refreshToken, TOKEN_TYPE, jwtService.getAccessTokenTtlSeconds());
    }
}
