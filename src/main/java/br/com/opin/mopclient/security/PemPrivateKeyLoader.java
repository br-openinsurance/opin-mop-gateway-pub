package br.com.opin.mopclient.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Loads a PKCS#8 private key from PEM.
 *
 * <p>Supported format:
 * <pre>
 * -----BEGIN PRIVATE KEY-----
 * (base64)
 * -----END PRIVATE KEY-----
 * </pre>
 */
public final class PemPrivateKeyLoader {

    private PemPrivateKeyLoader() {
    }

    public static PrivateKey loadPkcs8RsaPrivateKey(String pem) throws GeneralSecurityException {
        byte[] der = decodePem(pem, "PRIVATE KEY");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static byte[] decodePem(String pem, String type) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("Private key PEM is missing or blank");
        }

        String normalized = pem
                .replace("\r", "")
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");

        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PEM content: base64 decode failed", e);
        }
    }
}

