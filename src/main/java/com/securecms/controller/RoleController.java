package com.securecms.controller;

import com.securecms.dto.ApiResponse;
import com.securecms.dto.RoleResponse;
import com.securecms.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Available roles (ADMIN only)")
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all roles (ADMIN)")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.success("Roles retrieved successfully",
                roleRepository.findAll().stream().map(RoleResponse::from).toList());
    }
}
