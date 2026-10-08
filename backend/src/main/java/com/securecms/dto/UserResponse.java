package com.securecms.dto;

import java.time.LocalDateTime;

/** The phone is returned masked (e.g. ******3210). Passwords and keys are never exposed. */
public record UserResponse(Long id, String username, String email, String phone, String role, LocalDateTime createdAt) {
}
