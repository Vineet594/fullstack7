package com.securecms.dto;

import com.securecms.entity.RoleName;

import java.time.Instant;

/** Never contains the password hash. The phone number is returned masked. */
public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        RoleName role,
        Instant createdAt,
        Instant updatedAt) {
}
