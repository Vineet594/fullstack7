package com.securecms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Many posts belong to one author (User) and one Category. Both relations are LAZY and
 * unidirectional (no User.posts / Category.posts collections) so we never load big
 * collections by accident. Indexes support title search, FK joins and "newest first" sorting.
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_title", columnList = "title"),
        @Index(name = "idx_posts_category", columnList = "category_id"),
        @Index(name = "idx_posts_author", columnList = "author_id"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
