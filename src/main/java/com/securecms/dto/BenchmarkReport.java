package com.securecms.dto;

import java.time.Instant;
import java.util.List;

public record BenchmarkReport(Instant generatedAt, int pageSize, List<BenchmarkResult> results, String note) {
}
