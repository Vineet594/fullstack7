package com.securecms.service;

import com.securecms.dto.BenchmarkReport;
import com.securecms.dto.BenchmarkResult;
import com.securecms.dto.PostResponse;
import com.securecms.repository.PostRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Supplier;

/**
 * Measures REAL numbers (no faked values): elapsed time and the number of SQL statements Hibernate prepared.
 * Strategy A: findAll(Pageable) with LAZY associations -> the N+1 problem.
 * Strategy B: JOIN FETCH query -> everything in one statement (+1 count query for paging).
 * Note: Hibernate re-uses entities already loaded in the same session, so the "N" in N+1 equals the number of
 * DISTINCT authors/categories on the page, not the number of posts.
 */
@Service
public class BenchmarkService {

    private final PostRepository postRepository;
    private final Statistics statistics;
    private final TransactionTemplate transactionTemplate;

    public BenchmarkService(PostRepository postRepository, EntityManagerFactory entityManagerFactory,
                            PlatformTransactionManager transactionManager) {
        this.postRepository = postRepository;
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setReadOnly(true);
    }

    public BenchmarkReport compare(int size) {
        PageRequest page = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        BenchmarkResult naive = measure("Without optimization (lazy loading, N+1)", () ->
                postRepository.findAll(page).map(PostResponse::from).getContent());
        BenchmarkResult optimized = measure("With optimization (JOIN FETCH)", () ->
                postRepository.findAllWithDetails(page).map(PostResponse::from).getContent());
        return new BenchmarkReport(naive, optimized,
                "Numbers are measured live on this server. Repeat the call a few times: the first run includes JVM warm-up. "
                        + "Counters are global, so run it while no other traffic is hitting the API.");
    }

    private BenchmarkResult measure(String label, Supplier<List<PostResponse>> query) {
        return transactionTemplate.execute(status -> {
            statistics.setStatisticsEnabled(true);
            statistics.clear();
            long start = System.nanoTime();
            List<PostResponse> result = query.get();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            return new BenchmarkResult(label, result.size(), statistics.getPrepareStatementCount(), elapsedMs);
        });
    }

}
