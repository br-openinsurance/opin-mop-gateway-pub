package br.com.opin.mopclient.validator.infrastructure.config;



import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache manager configuration for the application.
 */
@Configuration
@EnableCaching
public class InMemoryCacheManagerConfig {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryCacheManagerConfig.class);

    public static final String OPEN_API_SPEC = "openApiSpec";
    public static final String NORMALIZED_ENDPOINTS = "normalizedEndpoints";
    public static final String ANONYMIZATION_CONFIG = "anonymizationConfig";

    @Value("${cache.open-api-spec.ttl-seconds:3600}")
    private long openApiSpecTtlSeconds;

    @Value("${cache.app-config.ttl-seconds:1800}")
    private long appConfigTtlSeconds;

    @Value("${cache.normalized-endpoints.ttl-seconds:300}")
    private long normalizedEndpointsTtlSeconds;

    @Value("${cache.max-size:10000}")
    private long maxCacheSize;

    @Bean
    public CacheManager cacheManager() {
        logger.info("Initializing Cache Manager with Caffeine...");
        
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<Cache> caches = new ArrayList<>();

        logger.info("  - Creating cache: {} (TTL: {} seconds, Max Size: 1)", OPEN_API_SPEC, openApiSpecTtlSeconds);
        caches.add(createCaffeineCache(OPEN_API_SPEC, Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(openApiSpecTtlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build()));

        logger.info("  - Creating cache: {} (TTL: {} seconds, Max Size: {})", NORMALIZED_ENDPOINTS, normalizedEndpointsTtlSeconds, maxCacheSize);
        caches.add(createCaffeineCache(NORMALIZED_ENDPOINTS, Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(normalizedEndpointsTtlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build()));

        // Anonymization configuration cache
        logger.info("  - Creating cache: {} (TTL: {} seconds, Max Size: 100)", ANONYMIZATION_CONFIG, appConfigTtlSeconds);
        caches.add(createCaffeineCache(ANONYMIZATION_CONFIG, Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(appConfigTtlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build()));

        cacheManager.setCaches(caches);
        logger.info("✓ Cache Manager initialized successfully with {} cache(s)", caches.size());
        return cacheManager;
    }

    private Cache createCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache) {
        return new Cache() {
            @Override
            @NonNull
            public String getName() {
                return Objects.requireNonNull(name, "Cache name cannot be null");
            }

            @Override
            @NonNull
            public Object getNativeCache() {
                return Objects.requireNonNull(caffeineCache, "Caffeine cache cannot be null");
            }

            @Override
            @Nullable
            public ValueWrapper get(@NonNull Object key) {
                Object value = caffeineCache.getIfPresent(key);
                return value != null ? () -> value : null;
            }

            @Override
            @SuppressWarnings("unchecked")
            @Nullable
            public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
                Object value = caffeineCache.getIfPresent(key);
                return type != null && type.isInstance(value) ? (T) value : null;
            }

            @Override
            @SuppressWarnings("unchecked")
            @NonNull
            public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
                Objects.requireNonNull(key, "Cache key cannot be null");
                Objects.requireNonNull(valueLoader, "Value loader cannot be null");
                T result = (T) caffeineCache.get(key, k -> {
                    try {
                        return valueLoader.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                return Objects.requireNonNull(result, "Cache value cannot be null");
            }

            @Override
            public void put(@NonNull Object key, @Nullable Object value) {
                caffeineCache.put(key, value);
            }

            @Override
            public void evict(@NonNull Object key) {
                caffeineCache.invalidate(key);
            }

            @Override
            public void clear() {
                caffeineCache.invalidateAll();
            }
        };
    }
}
