package com.securecms.dto;

import jakarta.validation.constraints.*;

public record PostRequest(
        @NotBlank(message = "Title cannot be blank") @Size(max = 150, message = "Title must be at most 150 characters") String title,
        @NotBlank(message = "Content cannot be blank") @Size(max = 5000, message = "Content must be at most 5000 characters") String content,
        @NotNull(message = "Category must be provided") @Min(value = 1, message = "Category id must be positive") Long categoryId) {
}
