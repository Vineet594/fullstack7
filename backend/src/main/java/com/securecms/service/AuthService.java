package com.securecms.service;

import com.securecms.dto.*;
import com.securecms.entity.RoleName;
import com.securecms.entity.User;
import com.securecms.exception.InvalidTokenException;
import com.securecms.exception.UnauthorizedException;
import com.securecms.mapper.UserMapper;
import com.securecms.repository.UserRepository;
import com.securecms.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Public registration always creates a normal USER; only an ADMIN can create other admins.
        return userService.createUser(request.username(), request.email(), request.password(),
                request.phone(), RoleName.USER);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Throws BadCredentialsException (-> 401 via GlobalExceptionHandler) when credentials are wrong.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public LoginResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.consume(request.refreshToken());   // validates + rotates
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private LoginResponse issueTokens(User user) {
        return new LoginResponse(jwtService.generateAccessToken(user), refreshTokenService.create(user),
                "Bearer", jwtService.getAccessTokenTtlSeconds(), userMapper.toResponse(user));
    }
}
