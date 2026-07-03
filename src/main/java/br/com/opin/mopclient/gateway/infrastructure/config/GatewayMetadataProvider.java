package br.com.opin.mopclient.gateway.infrastructure.config;

import br.com.opin.mopclient.MopClientApplication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves gateway metadata fields for outbound {@code MessageDTO} from build configuration.
 */
@Component
public class GatewayMetadataProvider {

    private static final String MAVEN_POM_PROPERTIES = "META-INF/maven/gateway/gateway/pom.properties";

    private final String version;
    private final String environment;

    public GatewayMetadataProvider(
            ObjectProvider<BuildProperties> buildPropertiesProvider,
            @Value("${mop.gateway.metadata.version:}") String configuredVersion,
            @Value("${mop.gateway.metadata.environment:}") String configuredEnvironment) {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        this.version = resolveVersion(configuredVersion, buildProperties);
        this.environment = resolveEnvironment(configuredEnvironment);
    }

    public String getVersion() {
        return version;
    }

    public String getEnvironment() {
        return environment;
    }

    private static String resolveVersion(String configuredVersion, BuildProperties buildProperties) {
        String resolved = firstResolvable(
                configuredVersion,
                buildProperties != null ? buildProperties.getVersion() : null,
                implementationVersion(),
                mavenPomProperty("version"));

        return resolved != null ? resolved : "unknown";
    }

    private static String resolveEnvironment(String configuredEnvironment) {
        String resolved = firstResolvable(configuredEnvironment);
        return resolved != null ? resolved : "unknown";
    }

    private static String firstResolvable(String... candidates) {
        for (String candidate : candidates) {
            if (isResolvable(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private static boolean isResolvable(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("@");
    }

    private static String implementationVersion() {
        Package applicationPackage = MopClientApplication.class.getPackage();
        if (applicationPackage == null) {
            return null;
        }
        return applicationPackage.getImplementationVersion();
    }

    private static String mavenPomProperty(String key) {
        try (InputStream input = GatewayMetadataProvider.class.getClassLoader()
                .getResourceAsStream(MAVEN_POM_PROPERTIES)) {
            if (input == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty(key);
        } catch (IOException ignored) {
            return null;
        }
    }
}
