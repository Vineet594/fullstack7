package com.securecms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private String uniqueName(String prefix) {
        return prefix + (System.nanoTime() % 1_000_000_000L);
    }

    @Test
    void register_withInvalidData_returnsValidationErrors() throws Exception {
        mockMvc.perform(jsonPost("/api/auth/register",
                        Map.of("username", "ab", "email", "not-an-email", "password", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void register_withValidData_createsUserWithoutExposingPassword() throws Exception {
        String username = uniqueName("student");

        mockMvc.perform(jsonPost("/api/auth/register", Map.of(
                        "username", username,
                        "email", username + "@example.com",
                        "password", "Strong@123",
                        "phone", "9876543210")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.phone").value("******3210"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void register_withExistingUsername_returnsConflict() throws Exception {
        mockMvc.perform(jsonPost("/api/auth/register", Map.of(
                        "username", "vineet", "email", "other@example.com", "password", "Strong@123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void login_withCorrectCredentials_returnsAccessAndRefreshTokens() throws Exception {
        mockMvc.perform(jsonPost("/api/auth/login", Map.of("username", "admin", "password", ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.refreshToken").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        mockMvc.perform(jsonPost("/api/auth/login", Map.of("username", "admin", "password", "Wrong@12345")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_rotatesTokens_andRejectsReuseOfTheOldToken() throws Exception {
        JsonNode tokens = login("rohan", USER_PASSWORD);
        String oldRefresh = tokens.get("refreshToken").asText();

        mockMvc.perform(jsonPost("/api/auth/refresh", Map.of("refreshToken", oldRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.refreshToken").value(not(oldRefresh)));

        mockMvc.perform(jsonPost("/api/auth/refresh", Map.of("refreshToken", oldRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_REVOKED"));
    }

    @Test
    void refresh_withUnknownToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(jsonPost("/api/auth/refresh", Map.of("refreshToken", "this-token-does-not-exist")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logout_revokesTheRefreshToken() throws Exception {
        String refresh = login("anita", USER_PASSWORD).get("refreshToken").asText();

        mockMvc.perform(jsonPost("/api/auth/logout", Map.of("refreshToken", refresh)))
                .andExpect(status().isNoContent());

        mockMvc.perform(jsonPost("/api/auth/refresh", Map.of("refreshToken", refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/posts"));
    }

    @Test
    void protectedEndpoint_withGarbageToken_returnsInvalidTokenError() throws Exception {
        mockMvc.perform(get("/api/posts").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void correlationId_isGeneratedWhenMissing_andReusedWhenProvided() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(header().string("X-Correlation-ID", not(emptyOrNullString())));

        mockMvc.perform(get("/api/posts").header("X-Correlation-ID", "viva-demo-123"))
                .andExpect(header().string("X-Correlation-ID", "viva-demo-123"));
    }
}
