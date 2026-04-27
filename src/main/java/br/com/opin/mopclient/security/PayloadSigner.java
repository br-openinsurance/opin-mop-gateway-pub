package br.com.opin.mopclient.security;

/**
 * Signs outbound request payloads before they are sent to downstream servers.
 */
public interface PayloadSigner {

    /**
     * Produces a compact signature token for the provided payload.
     *
     * @param payload raw request payload (bytes) to be signed
     * @return JWT compacto (ex.: PS256 + {@code typ: JWT}) a ser enviado ao servidor
     */
    String sign(byte[] payload);
}

