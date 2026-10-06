package com.securecms.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private Post existingPost;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setId(1L);
        author.setUsername("vineet");

        Category category = new Category();
        category.setId(10L);
        category.setName("Technology");

        existingPost = new Post();
        existingPost.setId(5L);
        existingPost.setTitle("Old title");
        existingPost.setContent("Old content");
        existingPost.setAuthor(author);
        existingPost.setCategory(category);
    }

    @Test
    void create_withUnknownCategory_throwsResourceNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.create(new PostRequest("Title", "Content", 99L),
                new AuthenticatedUser(1L, "vineet", "USER")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");

        verify(postRepository, never()).save(any());
    }

    @Test
    void update_byAnotherUser_isForbidden() {
        when(postRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(existingPost));

        assertThatThrownBy(() -> postService.update(5L, new PostRequest("New", "New content", 10L),
                new AuthenticatedUser(2L, "anita", "USER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_byAdmin_changesThePost() {
        when(postRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(existingPost));

        PostResponse response = postService.update(5L, new PostRequest("New title", "New content", 10L),
                new AuthenticatedUser(99L, "admin", "ADMIN"));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.content()).isEqualTo("New content");
    }

    @Test
    void delete_byOwner_removesThePost() {
        when(postRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(existingPost));

        postService.delete(5L, new AuthenticatedUser(1L, "vineet", "USER"));

        verify(postRepository).delete(existingPost);
    }

    @Test
    void delete_ofMissingPost_throwsResourceNotFound() {
        when(postRepository.findByIdWithDetails(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(404L, new AuthenticatedUser(1L, "vineet", "USER")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
