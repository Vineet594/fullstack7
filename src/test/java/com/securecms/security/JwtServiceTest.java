package com.securecms.security;

import com.securecms.entity.Role;
import com.securecms.entity.RoleName;
import com.securecms.entity.User;
import com.securecms.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-characters-long";

    private User sampleUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("vineet");
        user.setRole(new Role(RoleName.USER));
        return user;
    }

    @Test
    void generatedToken_containsUserIdUsernameAndRole() {
        JwtService jwtService = new JwtService(SECRET, 900);

        AuthenticatedUser parsed = jwtService.parseAccessToken(jwtService.generateAccessToken(sampleUser()));

        assertThat(parsed.getId()).isEqualTo(7L);
        assertThat(parsed.getUsername()).isEqualTo("vineet");
        assertThat(parsed.getRole()).isEqualTo("USER");
    }

    @Test
    void expiredToken_isRejectedWithTokenExpiredCode() {
        JwtService jwtService = new JwtService(SECRET, -10);
        String token = jwtService.generateAccessToken(sampleUser());

        assertThatThrownBy(() -> jwtService.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .extracting("errorCode").isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String token = new JwtService("another-secret-key-that-is-also-32-characters-long!", 900)
                .generateAccessToken(sampleUser());
        JwtService jwtService = new JwtService(SECRET, 900);

        assertThatThrownBy(() -> jwtService.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .extracting("errorCode").isEqualTo("INVALID_TOKEN");
    }

    @Test
    void garbageToken_isRejected() {
        JwtService jwtService = new JwtService(SECRET, 900);

        assertThatThrownBy(() -> jwtService.parseAccessToken("not.a.jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shortSecret_failsFast() {
        assertThatThrownBy(() -> new JwtService("too-short", 900)).isInstanceOf(IllegalStateException.class);
    }
}
