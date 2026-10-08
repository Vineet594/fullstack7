package com.securecms.service;

import com.securecms.dto.PostRequest;
import com.securecms.entity.Category;
import com.securecms.entity.Post;
import com.securecms.entity.User;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.repository.CategoryRepository;
import com.securecms.repository.PostRepository;
import com.securecms.repository.UserRepository;
import com.securecms.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @InjectMocks PostService postService;

    private Post postOwnedBy(long authorId) {
        Category category = Category.builder().id(1L).name("Backend").build();
        User author = User.builder().id(authorId).username("author" + authorId).build();
        return Post.builder().id(10L).title("Title").content("Content").category(category).author(author).build();
    }

    private final PostRequest request = new PostRequest("New title", "New content", 1L);

    @Test
    void userCannotUpdateSomeoneElsesPost() {
        when(postRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(postOwnedBy(1L)));
        AuthenticatedUser other = new AuthenticatedUser(2L, "other", "USER");
        assertThrows(AccessDeniedException.class, () -> postService.update(10L, request, other));
        verify(postRepository, never()).save(any());
    }

    @Test
    void adminCanUpdateAnyPost() {
        when(postRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(postOwnedBy(1L)));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        AuthenticatedUser admin = new AuthenticatedUser(99L, "admin", "ADMIN");
        assertEquals("New title", postService.update(10L, request, admin).title());
    }

    @Test
    void deletingMissingPostThrowsNotFound() {
        when(postRepository.findByIdWithDetails(404L)).thenReturn(Optional.empty());
        AuthenticatedUser admin = new AuthenticatedUser(99L, "admin", "ADMIN");
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> postService.delete(404L, admin));
        assertEquals("POST_NOT_FOUND", ex.getErrorCode());
    }
}
