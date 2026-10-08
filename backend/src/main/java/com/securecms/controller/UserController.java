package com.securecms.controller;

import com.securecms.dto.*;
import com.securecms.security.AuthenticatedUser;
import com.securecms.service.UserService;
import com.securecms.util.PageableFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "2. Users", description = "User management (mostly ADMIN only)")
public class UserController {

    private static final Set<String> SORT_FIELDS = Set.of("username", "email", "createdAt");

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Current user's profile (any authenticated user)")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok("Profile fetched successfully", userService.getById(principal.id()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users with pagination and sorting (ADMIN)")
    public ApiResponse<PageResponse<UserResponse>> list(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "field,direction. Allowed: username, email, createdAt") @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.ok("Users fetched successfully",
                userService.list(PageableFactory.create(page, size, sort, SORT_FIELDS)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a user by id (ADMIN). 404 if missing")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("User fetched successfully", userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user with a chosen role (ADMIN)")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("User created successfully", userService.createUser(request));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a user's role (ADMIN)")
    public ApiResponse<UserResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.ok("Role updated successfully", userService.updateRole(id, request.role()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user and their posts (ADMIN). 204 no content")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        userService.delete(id, principal.id());
    }
}
