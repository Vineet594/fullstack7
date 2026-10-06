package com.securecms.controller;

import com.securecms.dto.ApiResponse;
import com.securecms.dto.CategoryRequest;
import com.securecms.dto.CategoryResponse;
import com.securecms.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Post categories - reads are cached (Caffeine)")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List all categories", description = "Cached. First call = cache MISS (database), next calls = cache HIT.")
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.success("Categories retrieved successfully", categoryService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a category by id", description = "Cached per id.")
    public ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Category retrieved successfully", categoryService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category (ADMIN)", description = "Invalidates the cached category list.")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("Category created successfully", categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category (ADMIN)", description = "Refreshes the cached entry and invalidates the list cache.")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("Category updated successfully", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category (ADMIN)", description = "Fails with 409 if posts still use the category.")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
