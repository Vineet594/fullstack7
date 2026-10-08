package com.securecms.dto;

import jakarta.validation.constraints.*;

public record CategoryRequest(
        @NotBlank(message = "Category name cannot be blank") @Size(max = 60, message = "Name must be at most 60 characters") String name,
        @Size(max = 255, message = "Description must be at most 255 characters") String description) {
}
