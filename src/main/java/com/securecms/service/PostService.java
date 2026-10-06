package com.securecms.service;

import com.securecms.cache.CacheNames;
import com.securecms.dto.PageResponse;
import com.securecms.dto.PostRequest;
import com.securecms.dto.PostResponse;
import com.securecms.dto.PostSummaryResponse;
import com.securecms.entity.Category;
import com.securecms.entity.Post;
import com.securecms.entity.User;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.repository.CategoryRepository;
import com.securecms.repository.PostRepository;
import com.securecms.repository.UserRepository;
import com.securecms.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /** Uses JOIN FETCH queries: posts + author + category come back in ONE SQL statement (no N+1). */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> findAll(Long categoryId, Pageable pageable) {
        Page<Post> page = (categoryId == null)
                ? postRepository.findAllWithDetails(pageable)
                : postRepository.findAllByCategoryIdWithDetails(categoryId, pageable);
        return PageResponse.from(page.map(PostResponse::from));
    }

    /** Projection query: reads only the columns needed for a list screen. */
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> findAllSummaries(Pageable pageable) {
        return PageResponse.from(postRepository.findAllSummaries(pageable));
    }

    @Cacheable(cacheNames = CacheNames.POST, key = "#id")
    @Transactional(readOnly = true)
    public PostResponse findById(Long id) {
        log.info("Cache MISS - loading post {} from the database", id);
        return PostResponse.from(loadPostWithDetails(id));
    }

    @Transactional
    public PostResponse create(PostRequest request, AuthenticatedUser principal) {
        Category category = loadCategory(request.categoryId());
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        Post post = new Post();
        post.setTitle(request.title().trim());
        post.setContent(request.content());
        post.setCategory(category);
        post.setAuthor(author);

        Post saved = postRepository.save(post);
        log.info("Post created: id={} by userId={}", saved.getId(), principal.getId());
        return PostResponse.from(saved);
    }

    // @CachePut: always runs the method, then replaces the cached post with the fresh result
    @CachePut(cacheNames = CacheNames.POST, key = "#id")
    @Transactional
    public PostResponse update(Long id, PostRequest request, AuthenticatedUser principal) {
        Post post = loadPostWithDetails(id);
        assertCanModify(post, principal);

        if (!post.getCategory().getId().equals(request.categoryId())) {
            post.setCategory(loadCategory(request.categoryId()));
        }
        post.setTitle(request.title().trim());
        post.setContent(request.content());

        postRepository.flush(); // make sure updatedAt is generated before we build the response
        log.info("Post updated: id={} by userId={}", id, principal.getId());
        return PostResponse.from(post);
    }

    @CacheEvict(cacheNames = CacheNames.POST, key = "#id")
    @Transactional
    public void delete(Long id, AuthenticatedUser principal) {
        Post post = loadPostWithDetails(id);
        assertCanModify(post, principal);
        postRepository.delete(post);
        log.info("Post deleted: id={} by userId={}", id, principal.getId());
    }

    /** Ownership rule: ADMIN may change any post, USER only his own posts. */
    private void assertCanModify(Post post, AuthenticatedUser principal) {
        boolean isOwner = post.getAuthor().getId().equals(principal.getId());
        if (!principal.isAdmin() && !isOwner) {
            throw new AccessDeniedException("You can only modify your own posts");
        }
    }

    private Post loadPostWithDetails(Long id) {
        return postRepository.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    private Category loadCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
