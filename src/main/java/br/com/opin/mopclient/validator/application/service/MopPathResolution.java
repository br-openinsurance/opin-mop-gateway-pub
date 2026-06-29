package br.com.opin.mopclient.validator.application.service;

/**
 * Result of resolving a transmitter URL or path template to a MOP {@code path} header value.
 */
public record MopPathResolution(
        String mopPath,
        String basePath,
        String relativePath,
        String pathTemplate,
        String sourceFile,
        boolean matched) {
}
