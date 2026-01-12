package br.com.opin.mopclient.gateway.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    public static final String RABBIT_MQ_CONFIG = "rabbitMQConfig";

    @Bean
    public CacheManager cacheManager() {
        logger.info("Initializing Cache Manager...");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(RABBIT_MQ_CONFIG);
        logger.info("Cache Manager initialized successfully");
        logger.info("  - Configured cache: {}", RABBIT_MQ_CONFIG);
        return cacheManager;
    }
}
