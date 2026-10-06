package com.securecms.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.securecms.dto.CacheStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Reads the real hit/miss counters from Caffeine so cache behaviour can be demonstrated honestly. */
@Service
@RequiredArgsConstructor
public class CacheStatsService {

    private final CacheManager cacheManager;

    public List<CacheStatsResponse> getStats() {
        List<CacheStatsResponse> result = new ArrayList<>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache instanceof CaffeineCache caffeineCache) {
                CacheStats stats = caffeineCache.getNativeCache().stats();
                result.add(new CacheStatsResponse(name, stats.hitCount(), stats.missCount(),
                        stats.hitRate(), caffeineCache.getNativeCache().estimatedSize()));
            }
        }
        return result;
    }
}
