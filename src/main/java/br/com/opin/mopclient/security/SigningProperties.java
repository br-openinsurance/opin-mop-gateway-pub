package br.com.opin.mopclient.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for outbound payload signing.
 *
 * <p>Environment variables supported:
 * <ul>
 *   <li>{@code MOP_PAYLOAD_SIGNING_ENABLED} (true/false, optional)</li>
 *   <li>{@code MOP_PAYLOAD_SIGNING_PRIVATE_KEY_PEM} / {@code JWS_PRIVATE_KEY} (PEM PKCS#8)</li>
 *   <li>{@code MOP_PAYLOAD_SIGNING_KEY_ID} / {@code JWS_KID} (obrigatório com chave)</li>
 *   <li>{@code MOP_PAYLOAD_SIGNING_ORG_ID} / {@code JWS_ORG_ID} (obrigatório com chave)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "mop.payload-signing")
public class SigningProperties {

    /**
     * Enables request signing when a private key is present. If {@code null}, signing follows the presence
     * of {@link #privateKeyPem}. {@code false} is not allowed together with a non-blank private key
     * (application will fail to start).
     */
    private Boolean enabled;

    /**
     * Private key in PEM format (PKCS#8), typically loaded from an environment variable.
     */
    private String privateKeyPem;

    /**
     * Key id ({@code kid}) no header protegido do JWT (obrigatório quando há chave), alinhado a {@code JWS_KID}.
     */
    private String keyId;

    /**
     * Identificador da organização incluído nas claims ({@code orgId}), alinhado a {@code JWS_ORG_ID}.
     */
    private String orgId;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }
}

