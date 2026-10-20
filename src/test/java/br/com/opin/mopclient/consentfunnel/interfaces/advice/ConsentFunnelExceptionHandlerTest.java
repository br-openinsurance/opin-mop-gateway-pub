package br.com.opin.mopclient.consentfunnel.interfaces.advice;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@DisplayName("ConsentFunnelExceptionHandler")
class ConsentFunnelExceptionHandlerTest {

    private final ConsentFunnelExceptionHandler handler = new ConsentFunnelExceptionHandler();

    @Test
    @DisplayName("metrics 502 does not echo internal URL or downstream body")
    void metricsFailureDoesNotLeakInternalDetails() {
        var ex = new ErrorResponseException(
                "Connection error",
                "Unable to reach metrics endpoint: http://internal-mop/metrics");

        var response = handler.handleMetricsDeliveryFailure(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("Metrics delivery failed", response.getBody().get("error"));
        assertEquals("The consent funnel event could not be delivered.", response.getBody().get("details"));
        assertFalse(response.getBody().values().stream().anyMatch(v -> v.contains("internal-mop")));
    }

    @Test
    @DisplayName("unknown JSON property returns 400 funnel violation")
    void unknownPropertyReturnsFunnelBadRequest() {
        var funnel = ConsentFunnelValidationException.single(
                "orgId", "Unknown property 'orgId' is not allowed", "ADDITIONAL_PROPERTIES");
        var unreadable = new HttpMessageNotReadableException(
                "JSON parse error", funnel, mock(HttpInputMessage.class));

        var response = handler.handleUnreadableBody(unreadable);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("ERROR", response.getBody().getStatus());
        assertEquals(1, response.getBody().getTotal());
        assertEquals("orgId", response.getBody().getPending().get(0).getAttribute());
    }
}
