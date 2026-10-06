package com.securecms.repository;

import com.securecms.dto.PostSummaryResponse;
import com.securecms.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /*
     * GOOD (N+1 solution): one SQL statement loads posts + authors + categories using JOIN FETCH.
     * countQuery is required because Spring Data cannot derive a COUNT from a fetch-join query.
     */
    @Query(value = "select p from Post p join fetch p.author join fetch p.category",
            countQuery = "select count(p) from Post p")
    Page<Post> findAllWithDetails(Pageable pageable);

    @Query(value = "select p from Post p join fetch p.author join fetch p.category c where c.id = :categoryId",
            countQuery = "select count(p) from Post p where p.category.id = :categoryId")
    Page<Post> findAllByCategoryIdWithDetails(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("select p from Post p join fetch p.author join fetch p.category where p.id = :id")
    Optional<Post> findByIdWithDetails(@Param("id") Long id);

    /*
     * Projection: selects ONLY the 5 columns the list screen needs (no big "content" column).
     */
    @Query(value = "select new com.securecms.dto.PostSummaryResponse(p.id, p.title, a.username, c.name, p.createdAt) "
            + "from Post p join p.author a join p.category c",
            countQuery = "select count(p) from Post p")
    Page<PostSummaryResponse> findAllSummaries(Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}
