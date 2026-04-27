package br.com.opin.mopclient.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Assina o payload outbound como <strong>JWT</strong> compacto (PS256), alinhado ao fluxo típico com {@code jose}
 * ({@code SignJWT}): claims = objeto JSON do corpo + {@code orgId}, com {@code iat}, {@code exp} (1h),
 * header {@code alg: PS256}, {@code kid}, {@code typ: JWT}.
 */
public final class JwtPayloadSigner implements PayloadSigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtPayloadSigner.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RSAPrivateKey privateKey;
    private final String keyId;
    private final String orgId;

    public JwtPayloadSigner(RSAPrivateKey privateKey, String keyId, String orgId) {
        this.privateKey = Objects.requireNonNull(privateKey, "privateKey cannot be null");
        this.keyId = Objects.requireNonNull(keyId, "keyId (kid) cannot be null").trim();
        this.orgId = Objects.requireNonNull(orgId, "orgId cannot be null").trim();
        if (this.keyId.isEmpty()) {
            throw new IllegalArgumentException("keyId (kid) must not be blank (configure JWS_KID / mop.payload-signing.key-id)");
        }
        if (this.orgId.isEmpty()) {
            throw new IllegalArgumentException("orgId must not be blank (configure JWS_ORG_ID / mop.payload-signing.org-id)");
        }
    }

    public static JwtPayloadSigner fromPkcs8Pem(String privateKeyPem, String keyId, String orgId) {
        try {
            PrivateKey pk = PemPrivateKeyLoader.loadPkcs8RsaPrivateKey(privateKeyPem);
            if (!(pk instanceof RSAPrivateKey rsa)) {
                throw new IllegalArgumentException("Private key must be RSA (PKCS#8)");
            }
            return new JwtPayloadSigner(rsa, keyId, orgId);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to load RSA private key from PEM", e);
        }
    }

    @Override
    public String sign(byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[JWS] Iniciando assinatura | kid=\"{}\" orgId=\"{}\"", keyId, orgId);
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(new String(payload, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload must be valid JSON for JWT signing", e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Payload must be a JSON object (root {}) for JWT claims, like SignJWT({ ...payload, orgId })");
        }

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            if (v != null && !v.isNull()) {
                claimsBuilder.claim(e.getKey(), OBJECT_MAPPER.convertValue(v, Object.class));
            }
        }
        claimsBuilder.claim("orgId", orgId);
        Instant now = Instant.now();
        claimsBuilder.issueTime(Date.from(now));
        claimsBuilder.expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)));

        JWTClaimsSet claims = claimsBuilder.build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.PS256)
                .type(JOSEObjectType.JWT)
                .keyID(keyId)
                .build();

        try {
            SignedJWT signedJWT = new SignedJWT(header, claims);
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);
            String token = signedJWT.serialize();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[JWS] Token JWT gerado (kid: {})", keyId);
            }
            return token;
        } catch (JOSEException e) {
            LOGGER.error("[JWS] Falha ao gerar token JWT: {}", e.getMessage());
            throw new IllegalStateException("Failed to sign JWT: " + e.getMessage(), e);
        }
    }
}
