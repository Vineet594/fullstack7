package com.securecms.integration;

import com.securecms.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostIntegrationTest extends IntegrationTestSupport {

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void userCanCreatePostAndResponseUsesStandardStructure() throws Exception {
        String admin = tokenFor(RoleName.ADMIN);
        String user = tokenFor(RoleName.USER);
        long categoryId = createCategory(admin);

        mockMvc.perform(post("/api/posts").header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Hello", "content", "World", "categoryId", categoryId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post created successfully"))
                .andExpect(jsonPath("$.data.title").value("Hello"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void postCreationValidatesInput() throws Exception {
        String user = tokenFor(RoleName.USER);
        mockMvc.perform(post("/api/posts").header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.content").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());
    }

    @Test
    void paginationAndSortingWork() throws Exception {
        String admin = tokenFor(RoleName.ADMIN);
        long categoryId = createCategory(admin);
        for (String title : new String[]{"AAA post", "BBB post", "CCC post"}) {
            createPost(admin, categoryId, title);
        }
        mockMvc.perform(get("/api/posts?page=0&size=2&sort=title,asc").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    void pageSizeIsCappedAndInvalidSortFieldRejected() throws Exception {
        String admin = tokenFor(RoleName.ADMIN);
        mockMvc.perform(get("/api/posts?size=100000").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size", lessThanOrEqualTo(50)));
        mockMvc.perform(get("/api/posts?sort=password,asc").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SORT_FIELD"));
    }

    @Test
    void missingPostReturns404WithErrorCode() throws Exception {
        String user = tokenFor(RoleName.USER);
        mockMvc.perform(get("/api/posts/999999").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/posts/999999"));
    }

    @Test
    void userCannotDeleteAnotherUsersPostButAdminCan() throws Exception {
        String admin = tokenFor(RoleName.ADMIN);
        String owner = tokenFor(RoleName.USER);
        String stranger = tokenFor(RoleName.USER);
        long postId = createPost(owner, createCategory(admin), "Owned post");

        mockMvc.perform(delete("/api/posts/" + postId).header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/posts/" + postId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/" + postId).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalUserCannotCreateCategory() throws Exception {
        String user = tokenFor(RoleName.USER);
        mockMvc.perform(post("/api/categories").header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Hacking"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void correlationIdFromClientIsReturned() throws Exception {
        mockMvc.perform(get("/").header("X-Correlation-ID", "my-trace-id-12345"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "my-trace-id-12345"));
    }
}
