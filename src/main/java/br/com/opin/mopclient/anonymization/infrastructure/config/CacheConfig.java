package br.com.opin.mopclient.anonymization.infrastructure.config;

/**
 * Configuration constants for anonymization cache.
 * Note: The cache manager is defined in the Validator module ({@code InMemoryCacheManagerConfig}).
 */
public class CacheConfig {

    /**
     * Cache name for anonymization configuration.
     * Managed by the Validator module cache manager.
     */
    public static final String ANONYMIZATION_CONFIG = "anonymizationConfig";
}
