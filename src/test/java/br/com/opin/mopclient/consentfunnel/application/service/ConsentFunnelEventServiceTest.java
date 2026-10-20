package br.com.opin.mopclient.consentfunnel.application.service;

import br.com.opin.mopclient.consentfunnel.application.port.out.ConsentFunnelMetricsPort;
import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.infrastructure.validation.ConsentFunnelOpenApiValidator;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelAdditionalInfoDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventAcceptedResponseDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentFunnelEventService Tests")
class ConsentFunnelEventServiceTest {

    @Mock
    private ConsentFunnelOpenApiValidator openApiValidator;

    @Mock
    private ConsentFunnelMetricsPort metricsPort;

    @InjectMocks
    private ConsentFunnelEventService service;

    private static ConsentFunnelEventRequestDTO validRequest() {
        return ConsentFunnelEventRequestDTO.builder()
                .consentId("urn:bancoex:C1DD33123")
                .step("consent-created")
                .origin("CLIENT")
                .correlationId("577869e5-4c63-4b19-9235-a18d22c80986")
                .additionalInfo(ConsentFunnelAdditionalInfoDTO.builder()
                        .consentUser("user")
                        .build())
                .timestamp("2026-10-14T19:34:38")
                .clientSSId("2a59c2a3-529f-41c6-97e3-77395e9951ca")
                .serverASId("f8cd7b48-197d-419b-8680-f42226111b6f")
                .permissions(List.of("CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"))
                .build();
    }

    @Test
    @DisplayName("should accept valid event after OpenAPI validation")
    void shouldAcceptValidEvent() {
        doNothing().when(openApiValidator).validate(any(ConsentFunnelEventRequestDTO.class));
        doNothing().when(metricsPort).submit(any(), any());

        ConsentFunnelEventRequestDTO request = validRequest();
        ConsentFunnelEventAcceptedResponseDTO response = service.submit(request);

        assertNotNull(response);
        assertEquals(ConsentFunnelEventService.ACCEPTED_STATUS, response.getStatus());
        assertEquals("577869e5-4c63-4b19-9235-a18d22c80986", response.getCorrelationId());
        assertEquals("urn:bancoex:C1DD33123", response.getConsentId());
        assertEquals("consent-created", response.getStep());
        assertNotNull(response.getReceivedAt());
        assertTrue(response.getReceivedAt().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"));
        verify(metricsPort).submit(eq("577869e5-4c63-4b19-9235-a18d22c80986"), eq(request));
    }

    @Test
    @DisplayName("should propagate OpenAPI validation failures")
    void shouldPropagateOpenApiValidationFailures() {
        doThrow(ConsentFunnelValidationException.single("consentId", "Invalid consentId", "206"))
                .when(openApiValidator)
                .validate(any(ConsentFunnelEventRequestDTO.class));

        assertThrows(ConsentFunnelValidationException.class, () -> service.submit(validRequest()));
    }

    @Test
    @DisplayName("should reject blank required fields before OpenAPI")
    void shouldRejectBlankRequiredFields() {
        ConsentFunnelEventRequestDTO request = validRequest();
        request.setConsentId("   ");
        request.setClientSSId("");

        ConsentFunnelValidationException ex =
                assertThrows(ConsentFunnelValidationException.class, () -> service.submit(request));

        assertEquals(2, ex.getViolations().size());
        assertEquals("consentId", ex.getViolations().get(0).attribute());
        assertEquals("The field consentId is required and cannot be empty", ex.getViolations().get(0).message());
        assertEquals("clientSSId", ex.getViolations().get(1).attribute());
        verify(openApiValidator, never()).validate(any());
        verify(metricsPort, never()).submit(any(), any());
    }

    @Test
    @DisplayName("should accept event without additionalInfo")
    void shouldAcceptEventWithoutAdditionalInfo() {
        ConsentFunnelEventRequestDTO request = validRequest();
        request.setAdditionalInfo(null);
        doNothing().when(openApiValidator).validate(any(ConsentFunnelEventRequestDTO.class));
        doNothing().when(metricsPort).submit(any(), any());

        ConsentFunnelEventAcceptedResponseDTO response = service.submit(request);

        assertEquals(ConsentFunnelEventService.ACCEPTED_STATUS, response.getStatus());
        verify(metricsPort).submit(eq("577869e5-4c63-4b19-9235-a18d22c80986"), eq(request));
    }
}
