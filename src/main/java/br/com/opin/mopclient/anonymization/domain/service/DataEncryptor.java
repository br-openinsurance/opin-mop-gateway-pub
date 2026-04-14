package br.com.opin.mopclient.anonymization.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Domain service for encrypting data using hash algorithms.
 */
@Service
public class DataEncryptor {

    private static final Logger logger = LoggerFactory.getLogger(DataEncryptor.class);
    private static final String SALT = "open-insurance-mop-v1";

    public String encrypt(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (SALT + input).getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }
}
