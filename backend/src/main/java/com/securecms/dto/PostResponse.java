package com.securecms.dto;

import com.securecms.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(Long id, String title, String content, Long categoryId, String categoryName,
                           Long authorId, String authorUsername, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static PostResponse from(Post p) {
        return new PostResponse(p.getId(), p.getTitle(), p.getContent(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getAuthor().getId(), p.getAuthor().getUsername(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
