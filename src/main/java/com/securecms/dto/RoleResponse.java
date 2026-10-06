package com.securecms.dto;

import com.securecms.entity.Role;
import com.securecms.entity.RoleName;

public record RoleResponse(Long id, RoleName name) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName());
    }
}
