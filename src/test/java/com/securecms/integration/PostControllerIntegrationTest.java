package com.securecms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerIntegrationTest extends AbstractIntegrationTest {

    private long createPostAs(String bearer, String title) throws Exception {
        long categoryId = firstCategoryId(bearer);
        String body = mockMvc.perform(jsonPost("/api/posts",
                        Map.of("title", title, "content", "Some content", "categoryId", categoryId))
                        .header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("id").asLong();
    }

    @Test
    void createPost_asUser_setsLoggedInUserAsAuthor() throws Exception {
        String bearer = userBearer("vineet");
        long categoryId = firstCategoryId(bearer);

        mockMvc.perform(jsonPost("/api/posts",
                        Map.of("title", "My new post", "content", "Hello world", "categoryId", categoryId))
                        .header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("My new post"))
                .andExpect(jsonPath("$.data.authorUsername").value("vineet"));
    }

    @Test
    void createPost_withMissingFields_returnsValidationErrors() throws Exception {
        mockMvc.perform(jsonPost("/api/posts", Map.of("title", " ", "content", ""))
                        .header("Authorization", userBearer("vineet")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.content").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());
    }

    @Test
    void createCategory_asUser_isForbidden() throws Exception {
        mockMvc.perform(jsonPost("/api/categories", Map.of("name", "Hacked", "description", "x"))
                        .header("Authorization", userBearer("vineet")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void listUsers_asUser_isForbidden_butAdminCanDoIt() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", userBearer("anita")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header("Authorization", adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist());
    }

    @Test
    void createCategory_asAdmin_succeeds_andDuplicateIsRejected() throws Exception {
        String bearer = adminBearer();
        String name = "Category-" + (System.nanoTime() % 1_000_000_000L);

        mockMvc.perform(jsonPost("/api/categories", Map.of("name", name, "description", "demo"))
                        .header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(name));

        mockMvc.perform(jsonPost("/api/categories", Map.of("name", name, "description", "demo"))
                        .header("Authorization", bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void listPosts_isPaginated() throws Exception {
        mockMvc.perform(get("/api/posts?page=0&size=5").header("Authorization", userBearer("vineet")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(24)))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test
    void listPosts_pageSizeIsCappedAtFifty() throws Exception {
        mockMvc.perform(get("/api/posts?page=0&size=1000").header("Authorization", userBearer("vineet")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
    }

    @Test
    void listPosts_canBeSortedByTitle() throws Exception {
        String body = mockMvc.perform(get("/api/posts?page=0&size=20&sort=title,asc")
                        .header("Authorization", userBearer("vineet")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> titles = new ArrayList<>();
        for (JsonNode post : objectMapper.readTree(body).get("data").get("content")) {
            titles.add(post.get("title").asText());
        }
        List<String> sorted = new ArrayList<>(titles);
        sorted.sort(Comparator.naturalOrder());
        assertThat(titles).isEqualTo(sorted);
    }

    @Test
    void listPosts_withDisallowedSortField_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/posts?sort=content,asc").header("Authorization", userBearer("vineet")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SORT_FIELD"));
    }

    @Test
    void postSummaries_returnsProjection() throws Exception {
        mockMvc.perform(get("/api/posts/summaries?size=3&sort=title,asc").header("Authorization", userBearer("vineet")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].authorUsername").exists())
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());
    }

    @Test
    void getPost_thatDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/999999").header("Authorization", userBearer("vineet")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/posts/999999"));
    }

    @Test
    void updatePost_ownershipRulesAreEnforced() throws Exception {
        String owner = userBearer("vineet");
        String other = userBearer("anita");
        String admin = adminBearer();
        long postId = createPostAs(owner, "Ownership test");
        long categoryId = firstCategoryId(owner);
        Map<String, Object> update = Map.of("title", "Updated", "content", "Updated content", "categoryId", categoryId);

        mockMvc.perform(put("/api/posts/" + postId).header("Authorization", other)
                        .contentType("application/json").content(json(update)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/posts/" + postId).header("Authorization", owner)
                        .contentType("application/json").content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));

        mockMvc.perform(put("/api/posts/" + postId).header("Authorization", admin)
                        .contentType("application/json").content(json(Map.of("title", "Admin edit",
                                "content", "Edited by admin", "categoryId", categoryId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Admin edit"));
    }

    @Test
    void deletePost_byOwner_returnsNoContent_andPostIsGone() throws Exception {
        String owner = userBearer("rohan");
        long postId = createPostAs(owner, "Delete me");

        mockMvc.perform(delete("/api/posts/" + postId).header("Authorization", owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/" + postId).header("Authorization", owner))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPost_isServedFromCache_afterTheFirstCall() throws Exception {
        String admin = adminBearer();
        long postId = createPostAs(admin, "Cache demo post");

        mockMvc.perform(get("/api/posts/" + postId).header("Authorization", admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/posts/" + postId).header("Authorization", admin)).andExpect(status().isOk());

        String stats = mockMvc.perform(get("/api/admin/benchmark/cache-stats").header("Authorization", admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long postHits = 0;
        for (JsonNode cache : objectMapper.readTree(stats).get("data")) {
            if ("post".equals(cache.get("cacheName").asText())) {
                postHits = cache.get("hits").asLong();
            }
        }
        assertThat(postHits).isGreaterThanOrEqualTo(1);
    }

    @Test
    void benchmark_joinFetchUsesFewerSqlStatementsThanNPlusOne() throws Exception {
        String body = mockMvc.perform(get("/api/admin/benchmark/posts?size=20").header("Authorization", adminBearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode results = objectMapper.readTree(body).get("data").get("results");
        long naiveStatements = results.get(0).get("sqlStatements").asLong();
        long joinFetchStatements = results.get(1).get("sqlStatements").asLong();
        long cacheMiss = results.get(2).get("sqlStatements").asLong();
        long cacheHit = results.get(3).get("sqlStatements").asLong();

        assertThat(joinFetchStatements).isLessThan(naiveStatements);
        assertThat(cacheHit).isZero();
        assertThat(cacheMiss).isGreaterThan(0);
    }
}
