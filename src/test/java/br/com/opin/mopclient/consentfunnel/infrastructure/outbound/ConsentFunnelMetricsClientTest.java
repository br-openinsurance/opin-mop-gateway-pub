package br.com.opin.mopclient.consentfunnel.infrastructure.outbound;

import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelAdditionalInfoDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentFunnelMetricsClient Tests")
class ConsentFunnelMetricsClientTest {

    private static final String METRICS_URL =
            "https://mop-server.example.test/metrics";

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private ConsentFunnelMetricsClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new ConsentFunnelMetricsClient(restTemplate, objectMapper, METRICS_URL, true);
    }

    @Test
    @DisplayName("should POST serialized event to metrics URL when enabled")
    void shouldPostEventWhenEnabled() {
        ConsentFunnelEventRequestDTO event = validEvent();
        when(restTemplate.postForEntity(eq(METRICS_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.ACCEPTED));

        client.submit(event.getCorrelationId(), event);

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(METRICS_URL), captor.capture(), eq(String.class));
        assertEquals("application/json", captor.getValue().getHeaders().getContentType().toString());
    }

    @Test
    @DisplayName("should skip delivery when disabled")
    void shouldSkipWhenDisabled() {
        client = new ConsentFunnelMetricsClient(restTemplate, objectMapper, METRICS_URL, false);

        client.submit("corr-id", validEvent());

        verify(restTemplate, never()).postForEntity(any(), any(), any());
    }

    @Test
    @DisplayName("should fail when URL is blank and delivery is enabled")
    void shouldFailWhenUrlMissing() {
        client = new ConsentFunnelMetricsClient(restTemplate, objectMapper, "  ", true);

        assertThrows(IllegalStateException.class, () -> client.submit("corr-id", validEvent()));
    }

    @Test
    @DisplayName("should map connection errors to ErrorResponseException")
    void shouldMapConnectionErrors() {
        when(restTemplate.postForEntity(eq(METRICS_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        ErrorResponseException ex =
                assertThrows(ErrorResponseException.class, () -> client.submit("corr-id", validEvent()));

        assertEquals("Connection error", ex.getError());
    }

    private static ConsentFunnelEventRequestDTO validEvent() {
        return ConsentFunnelEventRequestDTO.builder()
                .consentId("urn:bancoex:C1DD33123")
                .step("consent-created")
                .origin("CLIENT")
                .correlationId("577869e5-4c63-4b19-9235-a18d22c80986")
                .additionalInfo(ConsentFunnelAdditionalInfoDTO.builder()
                        .consentUser("user")
                        .build())
                .timestamp("2022-11-07T17:26:32Z")
                .clientOrgId("1fb79963-4bff-4204-9370-93aceb8a2f0d")
                .clientSSId("2a59c2a3-529f-41c6-97e3-77395e9951ca")
                .serverOrgId("ff66b95a-d817-4fbe-949a-c5912e240189")
                .serverASId("f8cd7b48-197d-419b-8680-f42226111b6f")
                .permissions(List.of("CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"))
                .build();
    }
}
