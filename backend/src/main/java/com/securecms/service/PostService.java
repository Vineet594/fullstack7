package com.securecms.service;

import com.securecms.dto.PageResponse;
import com.securecms.dto.PostRequest;
import com.securecms.dto.PostResponse;
import com.securecms.entity.Category;
import com.securecms.entity.Post;
import com.securecms.entity.User;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.repository.CategoryRepository;
import com.securecms.repository.PostRepository;
import com.securecms.repository.UserRepository;
import com.securecms.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /** Not cached (every page/sort combination is different) but uses JOIN FETCH: 1 data query + 1 count query. */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(Pageable pageable) {
        return PageResponse.from(postRepository.findAllWithDetails(pageable).map(PostResponse::from));
    }

    @Cacheable(value = "posts", key = "#id")
    @Transactional(readOnly = true)
    public PostResponse getById(Long id) {
        return PostResponse.from(findWithDetails(id));
    }

    @Transactional
    public PostResponse create(PostRequest request, AuthenticatedUser currentUser) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND",
                        "Category not found with id " + request.categoryId()));
        User author = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Author no longer exists"));
        Post saved = postRepository.save(Post.builder()
                .title(request.title().trim()).content(request.content().trim())
                .category(category).author(author).build());
        return PostResponse.from(saved);
    }

    @CachePut(value = "posts", key = "#id")
    @Transactional
    public PostResponse update(Long id, PostRequest request, AuthenticatedUser currentUser) {
        Post post = findWithDetails(id);
        assertOwnerOrAdmin(post, currentUser);
        if (!post.getCategory().getId().equals(request.categoryId())) {
            post.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND",
                            "Category not found with id " + request.categoryId())));
        }
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        return PostResponse.from(postRepository.save(post));
    }

    @CacheEvict(value = "posts", key = "#id")
    @Transactional
    public void delete(Long id, AuthenticatedUser currentUser) {
        Post post = findWithDetails(id);
        assertOwnerOrAdmin(post, currentUser);
        postRepository.delete(post);
    }

    private Post findWithDetails(Long id) {
        return postRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("POST_NOT_FOUND", "Post not found with id " + id));
    }

    /** Authorization rule: ADMIN may change any post, USER only their own. */
    private void assertOwnerOrAdmin(Post post, AuthenticatedUser currentUser) {
        if (!currentUser.isAdmin() && !post.getAuthor().getId().equals(currentUser.id())) {
            throw new AccessDeniedException("Only the author or an admin can modify this post");
        }
    }
}
