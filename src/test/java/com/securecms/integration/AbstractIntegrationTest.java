package com.securecms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Starts the full application with an in-memory H2 database (profile "test") and the demo data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_PASSWORD = "Admin@12345";
    protected static final String USER_PASSWORD = "User@12345";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected MockHttpServletRequestBuilder jsonPost(String url, Object body) throws Exception {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    /** Logs in and returns the "data" node (accessToken, refreshToken, ...). */
    protected JsonNode login(String username, String password) throws Exception {
        String body = mockMvc.perform(jsonPost("/api/auth/login", Map.of("username", username, "password", password)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    protected String bearerFor(String username, String password) throws Exception {
        return "Bearer " + login(username, password).get("accessToken").asText();
    }

    protected String adminBearer() throws Exception {
        return bearerFor("admin", ADMIN_PASSWORD);
    }

    protected String userBearer(String username) throws Exception {
        return bearerFor(username, USER_PASSWORD);
    }

    protected long firstCategoryId(String bearer) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/categories").header("Authorization", bearer))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get(0).get("id").asLong();
    }
}
