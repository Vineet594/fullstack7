package com.securecms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank(message = "Post title cannot be blank")
        @Size(max = 150, message = "Post title must be at most 150 characters")
        String title,

        @NotBlank(message = "Post content cannot be blank")
        @Size(max = 5000, message = "Post content must be at most 5000 characters")
        String content,

        @NotNull(message = "Category must be provided")
        @Min(value = 1, message = "Category id must be a positive number")
        Long categoryId) {
}
