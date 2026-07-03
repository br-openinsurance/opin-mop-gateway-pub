package br.com.opin.mopclient.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GatewayMetadataProvider")
class GatewayMetadataProviderTest {

    @Test
    @DisplayName("uses configured version and environment from pom")
    void usesConfiguredMetadata() {
        GatewayMetadataProvider metadataProvider =
                new GatewayMetadataProvider(emptyBuildPropertiesProvider(), "1.0.5", "sandbox");

        assertEquals("1.0.5", metadataProvider.getVersion());
        assertEquals("sandbox", metadataProvider.getEnvironment());
    }

    @Test
    @DisplayName("ignores unresolved maven placeholders")
    void ignoresUnresolvedMavenPlaceholders() {
        Properties properties = new Properties();
        properties.setProperty("version", "1.0.5");
        BuildProperties buildProperties = new BuildProperties(properties);

        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(buildProperties);

        GatewayMetadataProvider metadataProvider =
                new GatewayMetadataProvider(provider, "@project.version@", "@mop.environment@");

        assertEquals("1.0.5", metadataProvider.getVersion());
        assertEquals("unknown", metadataProvider.getEnvironment());
    }

    @Test
    @DisplayName("uses build-info version when configured value is absent")
    void usesBuildInfoVersion() {
        Properties properties = new Properties();
        properties.setProperty("version", "1.0.5");
        BuildProperties buildProperties = new BuildProperties(properties);

        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(buildProperties);

        GatewayMetadataProvider metadataProvider =
                new GatewayMetadataProvider(provider, "", "sandbox");

        assertEquals("1.0.5", metadataProvider.getVersion());
        assertEquals("sandbox", metadataProvider.getEnvironment());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<BuildProperties> emptyBuildPropertiesProvider() {
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
