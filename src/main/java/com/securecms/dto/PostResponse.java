package com.securecms.dto;

import com.securecms.entity.Post;

import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long categoryId,
        String categoryName,
        Long authorId,
        String authorUsername,
        Instant createdAt,
        Instant updatedAt) {

    /** Expects category and author to be already loaded (JOIN FETCH) - otherwise it triggers lazy queries. */
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory().getId(),
                post.getCategory().getName(),
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
