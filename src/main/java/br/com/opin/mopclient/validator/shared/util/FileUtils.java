package br.com.opin.mopclient.validator.shared.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for file operations.
 */
public final class FileUtils {

    private FileUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Converts an InputStream to a temporary file.
     */
    public static File inputStreamToFile(InputStream inputStream, String fileName) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        File tempFile = File.createTempFile(fileName, ".yaml");
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            inputStream.close();
        }

        return tempFile;
    }
}
