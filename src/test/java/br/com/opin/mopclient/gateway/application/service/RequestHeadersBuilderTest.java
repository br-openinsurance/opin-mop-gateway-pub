package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.validator.application.service.MopPathResolution;
import br.com.opin.mopclient.validator.application.service.MopPathResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestHeadersBuilder Tests")
class RequestHeadersBuilderTest {

    private static final String MOP_PATH = "/open-insurance/consents/v3/consents";

    @Mock
    private TraceabilityService traceabilityService;

    @Mock
    private MopPathResolver mopPathResolver;

    @InjectMocks
    private RequestHeadersBuilder headersBuilder;

    @Test
    @DisplayName("Normalizes origin to lowercase client in outbound headers DTO")
    void shouldNormalizeClientOriginToLowercase() {
        stubTraceabilityAndPath();

        RequestHeadersDTO dto = headersBuilder.build(
                "corr-1",
                "CLIENT",
                MOP_PATH,
                "POST",
                "Request",
                null,
                null,
                Map.of(HttpHeaderConstants.ORIGIN, "CLIENT"),
                null,
                null);

        assertEquals("client", dto.getOrigin());
        assertEquals("client", dto.getHeaders().get(HttpHeaderConstants.ORIGIN));
        assertEquals("request", dto.getHttpType());
        assertEquals("request", dto.getHeaders().get(HttpHeaderConstants.HTTP_TYPE));
    }

    @Test
    @DisplayName("Normalizes origin to lowercase server in outbound headers DTO")
    void shouldNormalizeServerOriginToLowercase() {
        stubTraceabilityAndPath();

        RequestHeadersDTO dto = headersBuilder.build(
                "corr-2",
                " Server ",
                MOP_PATH,
                "POST",
                " Response ",
                "200",
                null,
                new HashMap<>(Map.of(
                        HttpHeaderConstants.ORIGIN, "Server",
                        HttpHeaderConstants.HTTP_TYPE, "Response")),
                null,
                null);

        assertEquals("server", dto.getOrigin());
        assertEquals("server", dto.getHeaders().get(HttpHeaderConstants.ORIGIN));
        assertEquals("response", dto.getHttpType());
        assertEquals("response", dto.getHeaders().get(HttpHeaderConstants.HTTP_TYPE));
    }

    @Test
    @DisplayName("normalizeHttpType maps request and response case-insensitively")
    void normalizeHttpTypeShouldMapKnownValues() {
        assertEquals("request", RequestHeadersBuilder.normalizeHttpType("REQUEST"));
        assertEquals("response", RequestHeadersBuilder.normalizeHttpType("Response"));
        assertEquals("request", RequestHeadersBuilder.normalizeHttpType(" request "));
    }

    @Test
    @DisplayName("normalizeOrigin maps client and server case-insensitively")
    void normalizeOriginShouldMapKnownValues() {
        assertEquals("client", RequestHeadersBuilder.normalizeOrigin("CLIENT"));
        assertEquals("server", RequestHeadersBuilder.normalizeOrigin("Server"));
        assertEquals("client", RequestHeadersBuilder.normalizeOrigin(" client "));
    }

    private void stubTraceabilityAndPath() {
        when(traceabilityService.getOrGenerateMopReportid(any())).thenReturn("report-1");
        when(traceabilityService.generateTimestamp()).thenReturn("2026-06-28T12:00:00Z");
        when(mopPathResolver.resolveFromTransmitterUrl(anyString()))
                .thenReturn(new MopPathResolution(MOP_PATH, null, null, null, null, true));
    }
}
