package com.securecms.controller;

import com.securecms.cache.CacheStatsService;
import com.securecms.dto.ApiResponse;
import com.securecms.dto.BenchmarkReport;
import com.securecms.dto.CacheStatsResponse;
import com.securecms.service.BenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/benchmark")
@RequiredArgsConstructor
@Tag(name = "Performance", description = "N+1 / caching benchmark with real measurements (ADMIN only)")
public class BenchmarkController {

    private static final int MAX_PAGE_SIZE = 50;

    private final BenchmarkService benchmarkService;
    private final CacheStatsService cacheStatsService;

    @GetMapping("/posts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run the N+1 vs JOIN FETCH vs cache benchmark",
            description = "Measures time and the real number of SQL statements for each approach on this machine.")
    public ApiResponse<BenchmarkReport> runBenchmark(@RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return ApiResponse.success("Benchmark completed", benchmarkService.run(safeSize));
    }

    @GetMapping("/cache-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cache hit / miss statistics", description = "Real counters from Caffeine.")
    public ApiResponse<List<CacheStatsResponse>> cacheStats() {
        return ApiResponse.success("Cache statistics retrieved successfully", cacheStatsService.getStats());
    }
}
