package com.securecms.dto;

import com.securecms.entity.Category;

public record CategoryResponse(Long id, String name, String description) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }
}
