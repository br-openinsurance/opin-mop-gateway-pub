package br.com.opin.mopclient.security.http;

import br.com.opin.mopclient.security.PayloadSigner;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayloadSigningInterceptorTest {

    @Test
    void shouldSendJwtBytesAndApplicationJwtContentType() throws IOException {
        String token = "signed-token-much-longer-than-original";
        PayloadSigner signer = body -> token;
        PayloadSigningInterceptor interceptor = new PayloadSigningInterceptor(signer, true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(3);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getHeaders()).thenReturn(headers);

        byte[] original = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
        byte[] expectedWire = token.getBytes(StandardCharsets.UTF_8);

        ClientHttpRequestExecution execution = (req, body) -> {
            assertArrayEquals(expectedWire, body);
            assertEquals(String.valueOf(expectedWire.length), req.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH));
            assertEquals(MediaType.valueOf("application/jwt"), req.getHeaders().getContentType());
            return mock(ClientHttpResponse.class);
        };

        interceptor.intercept(request, original, execution);
    }

    @Test
    void shouldRejectPostApplicationJsonWhenSigningDisabled() {
        PayloadSigner signer = body -> {
            throw new AssertionError("must not sign");
        };
        PayloadSigningInterceptor interceptor = new PayloadSigningInterceptor(signer, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getHeaders()).thenReturn(headers);

        assertThrows(
                IllegalStateException.class,
                () -> interceptor.intercept(
                        request,
                        "{}".getBytes(StandardCharsets.UTF_8),
                        (r, b) -> mock(ClientHttpResponse.class)));
    }

    @Test
    void shouldPassthroughPostJsonWhenAllowUnsignedPassthroughIsTrue() throws IOException {
        PayloadSigner signer = body -> {
            throw new AssertionError("must not sign in passthrough mode");
        };
        PayloadSigningInterceptor interceptor =
                new PayloadSigningInterceptor(signer, /*enabled=*/ false, /*allowUnsignedPassthrough=*/ true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(java.net.URI.create("https://mop.example/process"));

        byte[] bodyBytes = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        ClientHttpRequestExecution execution = (r, b) -> {
            assertArrayEquals(bodyBytes, b);
            assertEquals(MediaType.APPLICATION_JSON, r.getHeaders().getContentType());
            return response;
        };

        assertEquals(response, interceptor.intercept(request, bodyBytes, execution));
    }

    @Test
    void shouldPassthroughGetWhenSigningDisabled() throws IOException {
        PayloadSigner signer = body -> {
            throw new AssertionError("must not sign");
        };
        PayloadSigningInterceptor interceptor = new PayloadSigningInterceptor(signer, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders()).thenReturn(headers);

        byte[] bodyBytes = "{}".getBytes(StandardCharsets.UTF_8);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        ClientHttpRequestExecution execution = (r, b) -> {
            assertArrayEquals(bodyBytes, b);
            return response;
        };

        assertEquals(response, interceptor.intercept(request, bodyBytes, execution));
    }
}
