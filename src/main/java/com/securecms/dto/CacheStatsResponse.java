package com.securecms.dto;

public record CacheStatsResponse(String cacheName, long hits, long misses, double hitRate, long estimatedSize) {
}
