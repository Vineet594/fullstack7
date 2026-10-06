package com.securecms.service;

import com.securecms.cache.CacheNames;
import com.securecms.dto.BenchmarkReport;
import com.securecms.dto.BenchmarkResult;
import com.securecms.entity.Post;
import com.securecms.repository.PostRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Measures REAL numbers on the machine that runs the application:
 *  - elapsed time (System.nanoTime)
 *  - number of SQL statements (Hibernate Statistics: prepared statement count)
 * Nothing here is hard-coded or faked.
 */
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final EntityManagerFactory entityManagerFactory;
    private final PostRepository postRepository;
    private final PostService postService;
    private final CacheManager cacheManager;
    private final TransactionTemplate transactionTemplate;

    public BenchmarkReport run(int pageSize) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Warm-up (not measured): connection pool, JIT, Hibernate query plan cache
        naivePostList(pageable);
        optimizedPostList(pageable);

        List<BenchmarkResult> results = new java.util.ArrayList<>();

        results.add(measure(statistics, "1. WITHOUT optimisation",
                "findAll() then lazily touching author + category of every post (N+1 queries)",
                () -> naivePostList(pageable)));

        results.add(measure(statistics, "2. WITH JOIN FETCH",
                "One JOIN FETCH query loads posts + authors + categories",
                () -> optimizedPostList(pageable)));

        Page<Post> firstPost = postRepository.findAll(PageRequest.of(0, 1));
        if (firstPost.hasContent()) {
            Long postId = firstPost.getContent().get(0).getId();
            Cache postCache = cacheManager.getCache(CacheNames.POST);
            if (postCache != null) {
                postCache.evict(postId);
            }
            results.add(measure(statistics, "3. Single post - cache MISS",
                    "GET /api/posts/{id} when the post is not cached yet (database is queried)",
                    () -> postService.findById(postId) != null ? 1 : 0));
            results.add(measure(statistics, "4. Single post - cache HIT",
                    "Same call again: answered from Caffeine, no database call",
                    () -> postService.findById(postId) != null ? 1 : 0));
        }

        String note = "Numbers are measured live and vary per machine/run. Within one Hibernate session each distinct "
                + "author/category is loaded once, so the N+1 extra queries equal the number of DISTINCT authors and "
                + "categories on the page (more distinct authors = bigger difference). Run it several times and "
                + "compare; also try size=50.";
        return new BenchmarkReport(Instant.now(), pageSize, results, note);
    }

    /** BAD: 1 query for posts + 1 query for each distinct author + 1 for each distinct category. */
    private int naivePostList(Pageable pageable) {
        return transactionTemplate.execute(status -> {
            Page<Post> page = postRepository.findAll(pageable);
            page.forEach(post -> {
                post.getAuthor().getUsername();   // lazy load -> extra SELECT
                post.getCategory().getName();     // lazy load -> extra SELECT
            });
            return page.getNumberOfElements();
        });
    }

    /** GOOD: everything arrives in one JOIN query (+1 count query for pagination). */
    private int optimizedPostList(Pageable pageable) {
        return transactionTemplate.execute(status -> {
            Page<Post> page = postRepository.findAllWithDetails(pageable);
            page.forEach(post -> {
                post.getAuthor().getUsername();   // already loaded
                post.getCategory().getName();     // already loaded
            });
            return page.getNumberOfElements();
        });
    }

    private BenchmarkResult measure(Statistics statistics, String scenario, String description, IntSupplier action) {
        long statementsBefore = statistics.getPrepareStatementCount();
        long startNanos = System.nanoTime();
        int rows = action.getAsInt();
        double millis = (System.nanoTime() - startNanos) / 1_000_000.0;
        long statements = statistics.getPrepareStatementCount() - statementsBefore;
        return new BenchmarkResult(scenario, description, Math.round(millis * 100.0) / 100.0, statements, rows);
    }
}
