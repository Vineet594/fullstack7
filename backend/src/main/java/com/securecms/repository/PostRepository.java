package com.securecms.repository;

import com.securecms.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** GOOD: one SQL statement loads posts + author + category (JOIN FETCH). */
    @Query(value = "select p from Post p join fetch p.author join fetch p.category",
            countQuery = "select count(p) from Post p")
    Page<Post> findAllWithDetails(Pageable pageable);

    @Query("select p from Post p join fetch p.author join fetch p.category where p.id = :id")
    Optional<Post> findByIdWithDetails(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Post p where p.author.id = :authorId")
    void deleteByAuthorId(@Param("authorId") Long authorId);
}
