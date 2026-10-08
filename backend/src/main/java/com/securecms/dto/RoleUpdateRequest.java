package com.securecms.dto;

import com.securecms.entity.RoleName;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull(message = "Role is required") RoleName role) {
}
