package com.securecms.dto;

import com.securecms.entity.RoleName;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull(message = "Role must be provided (ADMIN or USER)") RoleName role) {
}
