package br.com.opin.mopclient.anonymization.interfaces.dto.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for MOP endpoints used by this gateway.
 * <p>
 * Maps the {@code mop.endpoints.*} subtree:
 * <ul>
 *   <li>{@code mop.endpoints.process.url} / {@code .method} — POST that delivers the anonymized payload to MOP.</li>
 *   <li>{@code mop.endpoints.anonymization-config.url} — GET that fetches the anonymization rules from MOP.</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "mop.endpoints")
public class MopEndpointsProperties {

    private Process process = new Process();
    private AnonymizationConfig anonymizationConfig = new AnonymizationConfig();

    @Data
    public static class Process {
        private String url;
        private String method;
    }

    @Data
    public static class AnonymizationConfig {
        private String url;
    }
}
