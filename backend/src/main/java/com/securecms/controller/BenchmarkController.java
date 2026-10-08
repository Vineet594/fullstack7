package com.securecms.controller;

import com.securecms.dto.ApiResponse;
import com.securecms.dto.BenchmarkReport;
import com.securecms.service.BenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
@Tag(name = "5. Performance", description = "Live comparison: N+1 queries vs JOIN FETCH (ADMIN)")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @GetMapping("/posts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Measure SQL statement count and time for lazy loading vs JOIN FETCH")
    public ApiResponse<BenchmarkReport> comparePostQueries(@RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        return ApiResponse.ok("Benchmark completed", benchmarkService.compare(safeSize));
    }
}
