package com.securecms.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

/** The "who is calling" object stored in the SecurityContext after the JWT has been validated. */
@Getter
@RequiredArgsConstructor
public final class AuthenticatedUser implements Principal {

    private final Long id;
    private final String username;
    private final String role;

    @Override
    public String getName() {
        return username;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
