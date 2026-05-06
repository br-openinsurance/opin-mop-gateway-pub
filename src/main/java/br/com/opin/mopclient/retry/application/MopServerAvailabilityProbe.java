package br.com.opin.mopclient.retry.application;

import br.com.opin.mopclient.retry.infrastructure.config.MopAvailabilityRestTemplateConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodically probes MOP (GET {@code mop.endpoints.anonymization-config.url}) without using the circuit breaker,
 * and exposes Prometheus-friendly gauges for availability and probe timestamps.
 */
@Component
public class MopServerAvailabilityProbe {

    private static final Logger logger = LoggerFactory.getLogger(MopServerAvailabilityProbe.class);

    private final RestTemplate restTemplate;
    private final String configUrl;
    private final String probeTargetLabel;
    private final boolean enabled;

    private final AtomicInteger available = new AtomicInteger(0);
    private final AtomicLong lastProbeEpochSeconds = new AtomicLong(0);
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong(0);

    /** {@code true} if the previous completed probe was successful (starts optimistic). */
    private boolean previousProbeOk = true;

    public MopServerAvailabilityProbe(
            @Qualifier(MopAvailabilityRestTemplateConfig.BEAN_NAME) RestTemplate restTemplate,
            @Value("${mop.endpoints.anonymization-config.url}") String configUrl,
            @Value("${mop.server.availability.enabled:true}") boolean enabled,
            MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.configUrl = configUrl;
        this.probeTargetLabel = buildProbeTargetLabel(configUrl);
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

    /** Whether periodic / initial HTTP probes are active ({@code mop.server.availability.enabled}). */
    public boolean isProbeEnabled() {
        return enabled;
    }

    /** URL completa usada no GET de disponibilidade ({@code mop.endpoints.anonymization-config.url}). */
    public String getAnonymizationConfigProbeUrl() {
        return configUrl;
    }

    /** Rótulo curto (host+caminho) alinhado com logs {@code [MOP availability]}. */
    public String getProbeTargetLabel() {
        return probeTargetLabel;
    }

    @PostConstruct
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
                onProbeSuccess(response.getStatusCode().toString());
            } else {
                available.set(0);
                onProbeFailure("http_status=" + response.getStatusCode());
            }
        } catch (ResourceAccessException e) {
            available.set(0);
            onProbeFailure(compactReason(e));
        } catch (Exception e) {
            available.set(0);
            onProbeFailure(compactReason(e));
        }
    }

    private void onProbeSuccess(String status) {
        if (!previousProbeOk) {
            logger.info("[MOP availability] UP | target={} | {}", probeTargetLabel, status);
        } else if (logger.isDebugEnabled()) {
            logger.debug("[MOP availability] OK | target={} | {}", probeTargetLabel, status);
        }
        previousProbeOk = true;
    }

    private void onProbeFailure(String reason) {
        if (previousProbeOk) {
            logger.warn("[MOP availability] DOWN | target={} | {}", probeTargetLabel, reason);
        } else {
            logger.debug("[MOP availability] still DOWN | target={} | {}", probeTargetLabel, reason);
        }
        previousProbeOk = false;
    }

    private static String buildProbeTargetLabel(String url) {
        try {
            URI u = URI.create(url);
            String host = u.getHost();
            String path = u.getPath();
            if (host != null && !host.isBlank()) {
                if (path != null && !path.isEmpty() && !"/".equals(path)) {
                    return host + path;
                }
                return host;
            }
            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                return "<no-host>" + path;
            }
            return "<invalid-url>";
        } catch (IllegalArgumentException e) {
            return url.length() > 80 ? url.substring(0, 77) + "..." : url;
        }
    }

    private static String compactReason(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String cls = root.getClass().getSimpleName();
        String msg = root.getMessage();
        if (msg == null || msg.isBlank()) {
            return cls;
        }
        String shortMsg = msg.replaceAll("\\s+", " ").trim();
        if (shortMsg.length() > 120) {
            shortMsg = shortMsg.substring(0, 117) + "...";
        }
        return cls + ": " + shortMsg;
    }
}
