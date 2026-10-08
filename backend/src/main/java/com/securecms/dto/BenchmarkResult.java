package com.securecms.dto;

public record BenchmarkResult(String strategy, int postsReturned, long sqlStatements, long executionTimeMs) {
}
