package br.com.opin.mopclient.retry.application;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import br.com.opin.mopclient.retry.infrastructure.config.MopAvailabilityRestTemplateConfig;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodically probes MOP (GET {@code external.api.data-anonymization}) without using the circuit breaker,
 * and exposes Prometheus-friendly gauges for availability and probe timestamps.
 */
@Component
public class MopServerAvailabilityProbe {

    private static final Logger logger = LoggerFactory.getLogger(MopServerAvailabilityProbe.class);

    private final RestTemplate restTemplate;
    private final String configUrl;
    private final boolean enabled;

    private final AtomicInteger available = new AtomicInteger(0);
    private final AtomicLong lastProbeEpochSeconds = new AtomicLong(0);
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong(0);

    public MopServerAvailabilityProbe(
            @Qualifier(MopAvailabilityRestTemplateConfig.BEAN_NAME) RestTemplate restTemplate,
            @Value("${external.api.data-anonymization}") String configUrl,
            @Value("${mop.server.availability.enabled:true}") boolean enabled,
            MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.configUrl = configUrl;
        this.enabled = enabled;

        Gauge.builder("mop.server.available", available, AtomicInteger::get)
                .description("1 if the last MOP availability probe succeeded, else 0")
                .register(meterRegistry);
        Gauge.builder("mop.server.last_probe.epoch.seconds", lastProbeEpochSeconds, AtomicLong::doubleValue)
                .description("Unix epoch seconds of the last completed probe (success or failure)")
                .register(meterRegistry);
        Gauge.builder("mop.server.last_success.epoch.seconds", lastSuccessEpochSeconds, AtomicLong::doubleValue)
                .description("Unix epoch seconds of the last successful probe (HTTP response received from MOP)")
                .register(meterRegistry);
    }

    /**
     * Reflects the outcome of the most recent probe. When checks are disabled, returns true so replay is not blocked.
     */
    public boolean isServerAvailable() {
        if (!enabled) {
            return true;
        }
        return available.get() == 1;
    }

    @jakarta.annotation.PostConstruct
    public void runInitialProbe() {
        runProbe();
    }

    @Scheduled(fixedDelayString = "${mop.server.availability.check-interval-ms:30000}", initialDelayString = "5000")
    public void runProbe() {
        if (!enabled) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        lastProbeEpochSeconds.set(now);
        try {
            var response = restTemplate.exchange(
                    configUrl,
                    HttpMethod.GET,
                    null,
                    String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                available.set(1);
                lastSuccessEpochSeconds.set(now);
                if (logger.isDebugEnabled()) {
                    logger.debug("MOP availability probe OK | status={}", response.getStatusCode());
                }
            } else {
                available.set(0);
                logger.warn("MOP availability probe: non-2xx status {} | url={}", response.getStatusCode(), configUrl);
            }
        } catch (ResourceAccessException e) {
            available.set(0);
            logger.warn("MOP availability probe: unreachable | url={} | {}", configUrl, e.getMessage());
        } catch (Exception e) {
            available.set(0);
            logger.warn("MOP availability probe: failed | url={} | {}", configUrl, e.getMessage());
        }
    }
}
