package br.com.opin.mopclient.retry.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Short-timeout {@link RestTemplate} for MOP reachability probes (not used by circuit-breaker clients).
 */
@Configuration
public class MopAvailabilityRestTemplateConfig {

    public static final String BEAN_NAME = "mopAvailabilityRestTemplate";

    @Bean(name = BEAN_NAME)
    @Qualifier(BEAN_NAME)
    public RestTemplate mopAvailabilityRestTemplate(
            RestTemplateBuilder builder,
            @Value("${mop.server.availability.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${mop.server.availability.read-timeout-ms:5000}") long readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
