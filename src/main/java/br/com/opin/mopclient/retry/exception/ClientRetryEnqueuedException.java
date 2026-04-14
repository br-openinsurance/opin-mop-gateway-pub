package br.com.opin.mopclient.retry.exception;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;

/**
 * Thrown after the request was persisted to the client retry queue because the MOP server
 * was unreachable (including circuit breaker open). The {@code successMessage} is typically the
 * same as the normal success body; unavailability is reflected in logs only.
 */
public class ClientRetryEnqueuedException extends RuntimeException {

    private final RequestHeadersDTO headersDTO;
    private final String successMessage;

    public ClientRetryEnqueuedException(RequestHeadersDTO headersDTO, String successMessage) {
        super(successMessage);
        this.headersDTO = headersDTO;
        this.successMessage = successMessage;
    }

    public RequestHeadersDTO getHeadersDTO() {
        return headersDTO;
    }

    public String getSuccessMessage() {
        return successMessage;
    }
}
