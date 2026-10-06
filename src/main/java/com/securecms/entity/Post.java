package com.securecms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A content post. Both associations are LAZY on purpose: this is what makes the
 * N+1 problem possible (and why JOIN FETCH is needed to fix it).
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_title", columnList = "title"),
        @Index(name = "idx_posts_category_id", columnList = "category_id"),
        @Index(name = "idx_posts_author_id", columnList = "author_id"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Post extends BaseEntity {

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
}
