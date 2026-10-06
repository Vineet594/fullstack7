package com.securecms.controller;

import com.securecms.dto.ApiResponse;
import com.securecms.dto.LoginRequest;
import com.securecms.dto.LoginResponse;
import com.securecms.dto.RefreshTokenRequest;
import com.securecms.dto.RegisterRequest;
import com.securecms.dto.UserResponse;
import com.securecms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh token, logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements // public endpoint: no lock icon in Swagger
    @Operation(summary = "Register a new account",
            description = "Creates a USER account. Password is stored as a BCrypt hash, phone as AES-GCM ciphertext.")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("User registered successfully", authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login",
            description = "Returns a short-lived JWT access token (15 min) and a long-lived refresh token (7 days).")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Get a new access token",
            description = "Send a valid refresh token. It is rotated: the old one is revoked and a new pair is returned.")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token refreshed successfully", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(summary = "Logout",
            description = "Revokes the given refresh token. The short-lived access token simply expires.")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
