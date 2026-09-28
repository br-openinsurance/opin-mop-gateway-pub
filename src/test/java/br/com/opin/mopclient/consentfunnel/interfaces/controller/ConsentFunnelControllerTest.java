package br.com.opin.mopclient.consentfunnel.interfaces.controller;

import br.com.opin.mopclient.consentfunnel.application.port.in.SubmitConsentFunnelEventUseCase;
import br.com.opin.mopclient.consentfunnel.application.service.ConsentFunnelEventService;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelAdditionalInfoDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventAcceptedResponseDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentFunnelController Tests")
class ConsentFunnelControllerTest {

    private static final String CORRELATION_ID = "577869e5-4c63-4b19-9235-a18d22c80986";
    private static final String CONSENT_ID = "urn:bancoex:C1DD33123";

    @Mock
    private SubmitConsentFunnelEventUseCase submitConsentFunnelEventUseCase;

    @InjectMocks
    private ConsentFunnelController controller;

    private static ConsentFunnelEventRequestDTO validRequest() {
        return ConsentFunnelEventRequestDTO.builder()
                .consentId(CONSENT_ID)
                .step("consent-created")
                .origin("CLIENT")
                .correlationId(CORRELATION_ID)
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

    @Nested
    @DisplayName("submitEvent")
    class SubmitEvent {

        @Test
        @DisplayName("should return 202 Accepted when use case succeeds")
        void shouldReturnAcceptedWhenUseCaseSucceeds() {
            ConsentFunnelEventRequestDTO request = validRequest();
            ConsentFunnelEventAcceptedResponseDTO accepted = ConsentFunnelEventAcceptedResponseDTO.builder()
                    .status(ConsentFunnelEventService.ACCEPTED_STATUS)
                    .correlationId(CORRELATION_ID)
                    .consentId(CONSENT_ID)
                    .step("consent-created")
                    .receivedAt("2026-07-29T12:00:00Z")
                    .build();

            when(submitConsentFunnelEventUseCase.submit(any())).thenReturn(accepted);

            ResponseEntity<ConsentFunnelEventAcceptedResponseDTO> response =
                    controller.submitEvent(request);

            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ConsentFunnelEventService.ACCEPTED_STATUS, response.getBody().getStatus());
            assertEquals(CORRELATION_ID, response.getBody().getCorrelationId());
            verify(submitConsentFunnelEventUseCase).submit(request);
        }
    }
}
