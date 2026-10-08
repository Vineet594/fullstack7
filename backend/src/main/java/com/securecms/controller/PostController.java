package com.securecms.controller;

import com.securecms.dto.*;
import com.securecms.security.AuthenticatedUser;
import com.securecms.service.PostService;
import com.securecms.util.PageableFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "3. Posts", description = "Content management. USER can change own posts, ADMIN any post")
public class PostController {

    private static final Set<String> SORT_FIELDS = Set.of("title", "createdAt", "updatedAt");

    private final PostService postService;

    @GetMapping
    @Operation(summary = "List posts with pagination and sorting")
    public ApiResponse<PageResponse<PostResponse>> list(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "field,direction. Allowed: title, createdAt, updatedAt") @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.ok("Posts fetched successfully",
                postService.list(PageableFactory.create(page, size, sort, SORT_FIELDS)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one post (cached). 404 if missing")
    public ApiResponse<PostResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Post fetched successfully", postService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a post (author = logged-in user)")
    public ApiResponse<PostResponse> create(@Valid @RequestBody PostRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok("Post created successfully", postService.create(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Update a post (author or ADMIN). 403 for other users")
    public ApiResponse<PostResponse> update(@PathVariable Long id, @Valid @RequestBody PostRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok("Post updated successfully", postService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a post (author or ADMIN). 204 no content")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        postService.delete(id, principal);
    }
}
