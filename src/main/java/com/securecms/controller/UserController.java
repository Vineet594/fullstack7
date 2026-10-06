package com.securecms.controller;

import com.securecms.dto.ApiResponse;
import com.securecms.dto.CreateUserRequest;
import com.securecms.dto.PageResponse;
import com.securecms.dto.RoleUpdateRequest;
import com.securecms.dto.UserResponse;
import com.securecms.security.AuthenticatedUser;
import com.securecms.service.UserService;
import com.securecms.util.SortValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management (mostly ADMIN only)")
public class UserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("username", "email", "createdAt");

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Any authenticated user. Phone is returned masked.")
    public ApiResponse<UserResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success("Profile retrieved successfully", userService.getProfile(principal));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (ADMIN)",
            description = "Paginated. Sort fields: username, email, createdAt. Example: ?page=0&size=10&sort=username,asc")
    public ApiResponse<PageResponse<UserResponse>> list(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SortValidator.validate(pageable, ALLOWED_SORT_FIELDS);
        return ApiResponse.success("Users retrieved successfully", PageResponse.from(userService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Operation(summary = "Get a user by id", description = "ADMIN can read anyone, a USER can read only himself.")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("User retrieved successfully", userService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a user with a role (ADMIN)")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created successfully", userService.createUser(
                request.username(), request.email(), request.password(), request.phone(), request.role()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change the role of a user (ADMIN)",
            description = "PATCH = partial update. The user's refresh tokens are revoked so the new role applies after re-login.")
    public ApiResponse<UserResponse> changeRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success("User role updated successfully", userService.changeRole(id, request.role()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a user (ADMIN)", description = "Also deletes the user's posts. You cannot delete yourself.")
    public void delete(@PathVariable Long id,
                       @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        userService.delete(id, principal);
    }
}
