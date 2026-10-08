package com.securecms.security;

/** The principal stored in the SecurityContext after a JWT has been validated. */
public record AuthenticatedUser(Long id, String username, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
