package br.com.opin.mopclient.security.config;

import br.com.opin.mopclient.security.JwtPayloadSigner;
import br.com.opin.mopclient.security.PayloadSigner;
import br.com.opin.mopclient.security.SigningProperties;
import br.com.opin.mopclient.security.http.PayloadSigningInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Spring configuration that wires outbound payload signing.
 */
@Configuration
@EnableConfigurationProperties(SigningProperties.class)
public class PayloadSigningConfig {

    @Bean
    public PayloadSigner payloadSigner(SigningProperties props) {
        return JwtPayloadSigner.fromPkcs8Pem(
                props.getPrivateKeyPem(),
                props.getKeyId() != null ? props.getKeyId() : "",
                props.getOrgId() != null ? props.getOrgId() : "");
    }

    @Bean
    public PayloadSigningInterceptor payloadSigningInterceptor(SigningProperties props, PayloadSigner signer) {
        boolean hasKey = StringUtils.hasText(props.getPrivateKeyPem());
        if (Boolean.FALSE.equals(props.getEnabled()) && hasKey) {
            throw new IllegalStateException(
                    "mop.payload-signing.enabled=false cannot be used when mop.payload-signing.private-key-pem is set; "
                            + "the gateway must not send cleartext JSON to MOP.");
        }
        boolean enabled = hasKey && !Boolean.FALSE.equals(props.getEnabled());
        return new PayloadSigningInterceptor(signer, enabled);
    }
}

