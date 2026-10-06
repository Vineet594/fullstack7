package com.securecms.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Enables Spring Cache with Caffeine (in-memory, runs locally - no Redis needed).
 * - max 1000 entries per cache
 * - entries expire 10 minutes after being written (safety net on top of explicit eviction)
 * - recordStats() lets us show hits/misses on the admin cache-stats endpoint
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CacheNames.CATEGORIES, CacheNames.CATEGORY, CacheNames.POST);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        return manager;
    }
}
