package br.com.opin.mopclient.gateway.infrastructure.config;

/**
 * Cache configuration constants.
 * Note: The cache manager is defined in the Validator module ({@code InMemoryCacheManagerConfig}).
 * Holds compatibility constants only.
 */
public class CacheConfig {

    /**
     * Cache name for application configuration (legacy, kept for compatibility).
     * Managed by the Validator module cache manager.
     */
    public static final String APP_CONFIG = "appConfig";
}
