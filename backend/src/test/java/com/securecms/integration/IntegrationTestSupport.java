package com.securecms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securecms.entity.RoleName;
import com.securecms.entity.User;
import com.securecms.repository.RoleRepository;
import com.securecms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "Passw0rd!x";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected String uniqueName() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** Inserts a user directly in the DB and returns its username. */
    protected String createUser(RoleName role) {
        String username = uniqueName();
        userRepository.save(User.builder().username(username).email(username + "@test.dev")
                .password(passwordEncoder.encode(PASSWORD))
                .role(roleRepository.findByName(role).orElseThrow()).build());
        return username;
    }

    protected JsonNode login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    protected String tokenFor(RoleName role) throws Exception {
        return login(createUser(role)).get("accessToken").asText();
    }

    protected long createCategory(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Cat-" + uniqueName(), "description", "test"))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    protected long createPost(String token, long categoryId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", title, "content", "Some content", "categoryId", categoryId))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
    }
}
