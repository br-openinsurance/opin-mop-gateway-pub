package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Server;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Binds {@code *_vN.yaml} companions (in {@code swagger/current/} or {@code swagger/version/})
 * to the API version segment used in the MOP path.
 */
final class OpenApiSpecFileVersion {

    private static final Pattern FILE_VERSION = Pattern.compile("^(.+)_v(\\d+)\\.ya?ml$", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_VERSION_SEGMENT = Pattern.compile("/v\\d+(?=/|$)");

    private OpenApiSpecFileVersion() {
    }

    static Optional<Integer> versionFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = FILE_VERSION.matcher(fileName.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group(2)));
    }

    static Optional<String> canonicalFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = FILE_VERSION.matcher(fileName.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1) + ".yaml");
    }

    static String applyToBasePath(String fileName, String basePath) {
        Optional<Integer> version = versionFromFileName(fileName);
        if (version.isEmpty()) {
            return OpenApiPathMatcher.normalizePath(basePath);
        }
        return rewriteVersionSegment(basePath, version.get());
    }

    static void applyToParsedSpec(OpenApi3 openApi, String fileName) {
        if (openApi == null) {
            return;
        }
        Optional<Integer> version = versionFromFileName(fileName);
        if (version.isEmpty()) {
            return;
        }
        List<Server> servers = openApi.getServers();
        if (servers == null || servers.isEmpty()) {
            return;
        }
        for (Server server : servers) {
            if (server.getUrl() == null || server.getUrl().isBlank()) {
                continue;
            }
            server.setUrl(rewriteVersionSegment(server.getUrl(), version.get()));
        }
    }

    static String rewriteVersionSegment(String urlOrPath, int version) {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            return urlOrPath;
        }
        Matcher matcher = URL_VERSION_SEGMENT.matcher(urlOrPath);
        StringBuffer rewritten = new StringBuffer();
        boolean replaced = false;
        while (matcher.find()) {
            matcher.appendReplacement(rewritten, "/v" + version);
            replaced = true;
        }
        matcher.appendTail(rewritten);
        if (!replaced) {
            return urlOrPath;
        }
        if (urlOrPath.contains("://")) {
            return rewritten.toString();
        }
        return OpenApiPathMatcher.normalizePath(rewritten.toString());
    }
}
