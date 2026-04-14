package br.com.opin.mopclient.anonymization.interfaces.dto.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for external server request settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "external.request")
public class ServerRequestProperties {

    private String url;
    private String host;
    private String path;
    private String method;
}
