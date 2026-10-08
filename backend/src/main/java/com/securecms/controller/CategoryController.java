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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "4. Categories", description = "Everyone can read (cached), only ADMIN can manage")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all categories (cached)")
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok("Categories fetched successfully", categoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category (cached). 404 if missing")
    public ApiResponse<CategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Category fetched successfully", categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category (ADMIN). 409 if name exists")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category created successfully", categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category (ADMIN) - refreshes/evicts caches")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category updated successfully", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category (ADMIN). 409 if it still has posts")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
