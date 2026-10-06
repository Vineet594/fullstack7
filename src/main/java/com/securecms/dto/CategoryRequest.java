package com.securecms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name cannot be blank")
        @Size(max = 100, message = "Category name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description) {
}
