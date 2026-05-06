package br.com.opin.mopclient.gateway.infrastructure.config;

import java.net.URI;

/**
 * Validates HTTP(S) URLs resolved at runtime (e.g. MOP endpoints) for startup diagnostics.
 */
public final class StartupHttpUrlSupport {

    private StartupHttpUrlSupport() {
    }

    /**
     * @param rawUrl property value; may contain unresolved placeholders if env/YAML is wrong
     */
    public static Assessment assess(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return new Assessment(false, "empty_url", null);
        }
        String trimmed = rawUrl.trim();
        if (trimmed.contains("${")) {
            return new Assessment(false, "unresolved_placeholder", trimmed);
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return new Assessment(false, "invalid_scheme", trimmed);
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return new Assessment(false, "missing_host", trimmed);
            }
            return new Assessment(true, null, trimmed);
        } catch (IllegalArgumentException e) {
            return new Assessment(false, "bad_uri", trimmed);
        }
    }

    public record Assessment(boolean valid, String issueKey, String url) {

        public String statusLine() {
            if (valid) {
                return "OK";
            }
            return "MISCONFIGURED (" + (issueKey != null ? issueKey : "unknown") + ")";
        }
    }
}
