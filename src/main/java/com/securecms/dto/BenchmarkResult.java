package com.securecms.dto;

/** One measured scenario. All numbers are measured live (never hard-coded). */
public record BenchmarkResult(
        String scenario,
        String description,
        double durationMillis,
        long sqlStatements,
        int rowsReturned) {
}
