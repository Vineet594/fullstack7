package com.securecms.dto;

public record BenchmarkReport(BenchmarkResult withoutOptimization, BenchmarkResult withJoinFetch, String note) {
}
