package br.com.opin.mopclient.security.config;

import br.com.opin.mopclient.security.JwtPayloadSigner;
import br.com.opin.mopclient.security.PayloadSigner;
import br.com.opin.mopclient.security.SigningProperties;
import br.com.opin.mopclient.security.http.PayloadSigningInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Assinatura de saída no {@code RestTemplate} compartilhado:
 * rastreio {@code POST /process} e funil {@code POST} em {@code MOP_CONSENT_FUNNEL_METRICS_URL}.
 *
 * <ul>
 *   <li>{@code MOP_PAYLOAD_SIGNING_ENABLED=true} — JWT PS256 ({@code application/jwt}).</li>
 *   <li>{@code MOP_PAYLOAD_SIGNING_ENABLED=false} — JSON em claro nos dois canais (assinatura na borda).</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(SigningProperties.class)
public class PayloadSigningConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadSigningConfig.class);

    @Bean
    public PayloadSigner payloadSigner(SigningProperties props) {
        if (!StringUtils.hasText(props.getPrivateKeyPem())) {
            // Sem chave: signer "stub" que nunca deve ser invocado em runtime.
            // O interceptor é configurado em modo passthrough e não chama este bean.
            return body -> {
                throw new IllegalStateException(
                        "PayloadSigner invoked without a configured private key. "
                                + "Configure JWS_PRIVATE_KEY/JWS_KID/JWS_ORG_ID for production.");
            };
        }
        return JwtPayloadSigner.fromPkcs8Pem(
                props.getPrivateKeyPem(),
                props.getKeyId() != null ? props.getKeyId() : "",
                props.getOrgId() != null ? props.getOrgId() : "");
    }

    @Bean
    public PayloadSigningInterceptor payloadSigningInterceptor(SigningProperties props, PayloadSigner signer) {
        boolean hasKey = StringUtils.hasText(props.getPrivateKeyPem());
        Boolean enabledProp = props.getEnabled();

        final boolean enabled;
        final boolean allowUnsignedPassthrough;

        if (Boolean.TRUE.equals(enabledProp)) {
            require(hasKey, "MOP_PAYLOAD_SIGNING_ENABLED=true requires JWS_PRIVATE_KEY");
            requireText(props.getKeyId(), "MOP_PAYLOAD_SIGNING_ENABLED=true requires JWS_KID");
            requireText(props.getOrgId(), "MOP_PAYLOAD_SIGNING_ENABLED=true requires JWS_ORG_ID");
            enabled = true;
            allowUnsignedPassthrough = false;
        } else if (Boolean.FALSE.equals(enabledProp)) {
            requireText(props.getOrgId(), "MOP_PAYLOAD_SIGNING_ENABLED=false still requires JWS_ORG_ID");
            enabled = false;
            allowUnsignedPassthrough = true;
            if (hasKey) {
                LOGGER.warn(
                        "[JWS] MOP_PAYLOAD_SIGNING_ENABLED=false: POST /process and funnel /metrics "
                                + "leave as unsigned JSON (signing at the edge).");
            }
        } else {
            // enabled=null: follow the presence of the key (default behavior)
            if (hasKey) {
                requireText(props.getKeyId(), "JWS_PRIVATE_KEY is set, so JWS_KID is required");
                requireText(props.getOrgId(), "JWS_PRIVATE_KEY is set, so JWS_ORG_ID is required");
            }
            enabled = hasKey;
            allowUnsignedPassthrough = !hasKey;
        }

        if (allowUnsignedPassthrough) {
            LOGGER.warn(
                    "[JWS] No private key configured (JWS_PRIVATE_KEY empty). Outbound payloads will be sent UNSIGNED. "
                            + "This mode is intended for local development only — MOP will likely respond 401. "
                            + "Configure JWS_PRIVATE_KEY/JWS_KID/JWS_ORG_ID before going to production.");
        }
        return new PayloadSigningInterceptor(signer, enabled, allowUnsignedPassthrough);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }
}
