package com.securecms.controller;

import com.securecms.dto.ApiResponse;
import com.securecms.dto.PageResponse;
import com.securecms.dto.PostRequest;
import com.securecms.dto.PostResponse;
import com.securecms.dto.PostSummaryResponse;
import com.securecms.security.AuthenticatedUser;
import com.securecms.service.PostService;
import com.securecms.util.SortValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Content posts with pagination, sorting and ownership rules")
public class PostController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "createdAt", "updatedAt");

    private final PostService postService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List posts (paginated + sorted)",
            description = "Example: ?page=0&size=10&sort=createdAt,desc. Max page size is 50. "
                    + "Sort fields: title, createdAt, updatedAt. Optional filter: categoryId. Uses JOIN FETCH (no N+1).")
    public ApiResponse<PageResponse<PostResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SortValidator.validate(pageable, ALLOWED_SORT_FIELDS);
        return ApiResponse.success("Posts retrieved successfully", postService.findAll(categoryId, pageable));
    }

    @GetMapping("/summaries")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List post summaries (projection)",
            description = "Reads only id, title, author, category and date - the large content column is not loaded.")
    public ApiResponse<PageResponse<PostSummaryResponse>> summaries(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SortValidator.validate(pageable, ALLOWED_SORT_FIELDS);
        return ApiResponse.success("Post summaries retrieved successfully", postService.findAllSummaries(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a post by id", description = "Cached (@Cacheable).")
    public ApiResponse<PostResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Post retrieved successfully", postService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Create a post", description = "The author is always the logged-in user.")
    public ApiResponse<PostResponse> create(@Valid @RequestBody PostRequest request,
                                            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success("Post created successfully", postService.create(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Update a post", description = "ADMIN: any post. USER: only own posts (otherwise 403).")
    public ApiResponse<PostResponse> update(@PathVariable Long id, @Valid @RequestBody PostRequest request,
                                            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success("Post updated successfully", postService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Delete a post", description = "ADMIN: any post. USER: only own posts (otherwise 403).")
    public void delete(@PathVariable Long id,
                       @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        postService.delete(id, principal);
    }
}
