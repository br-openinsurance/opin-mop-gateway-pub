package br.com.opin.mopclient.security.http;

import br.com.opin.mopclient.anonymization.shared.util.MopReportidManager;
import br.com.opin.mopclient.security.PayloadSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Substitui o corpo JSON do POST pelo <strong>JWT compacto</strong> assinado e define
 * {@code Content-Type: application/jwt}, conforme exigido pelo endpoint {@code /process} do MOP.
 *
 * <p>O JWT é gerado a partir do JSON original (claims em {@link br.com.opin.mopclient.security.JwtPayloadSigner}).
 * O JSON em claro não é enviado na rede.</p>
 */
public final class PayloadSigningInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadSigningInterceptor.class);

    private final PayloadSigner signer;
    private final boolean enabled;

    public PayloadSigningInterceptor(PayloadSigner signer, boolean enabled) {
        this.signer = Objects.requireNonNull(signer, "signer cannot be null");
        this.enabled = enabled;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        if (!enabled) {
            if (wouldSendUnsignedJson(request, body)) {
                throw new IllegalStateException(
                        "Outbound payload signing is disabled but a JSON body would be sent unsigned. "
                                + "Configure mop.payload-signing.private-key-pem (and do not set enabled=false when the key is present).");
            }
            return execution.execute(request, body);
        }

        HttpMethod method = request.getMethod();
        if (method == null || method == HttpMethod.GET || method == HttpMethod.HEAD) {
            return execution.execute(request, body);
        }

        Assert.notNull(body, "body must not be null");
        String token = signer.sign(body);

        byte[] signedBody = token.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = request.getHeaders();
        headers.setContentType(MediaType.valueOf("application/jwt"));
        headers.setContentLength(signedBody.length);

        LOGGER.info(
                "[STEP 6.3] Outbound: application/jwt | method={} | URI={} | jwtLength={} | Correlation ID: {} | jwt={}",
                method,
                request.getURI(),
                signedBody.length,
                correlationIdOrDash(),
                token);

        return execution.execute(request, signedBody);
    }

    private static String correlationIdOrDash() {
        String id = MopReportidManager.getMopReportid();
        return id != null && !id.isBlank() ? id : "n/a";
    }

    private static boolean wouldSendUnsignedJson(HttpRequest request, byte[] body) {
        if (body == null || body.length == 0) {
            return false;
        }
        HttpMethod method = request.getMethod();
        if (method == null
                || !(method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
            return false;
        }
        MediaType contentType = request.getHeaders().getContentType();
        return contentType != null && MediaType.APPLICATION_JSON.includes(contentType);
    }
}
