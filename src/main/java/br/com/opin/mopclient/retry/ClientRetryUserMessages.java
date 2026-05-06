package br.com.opin.mopclient.retry;

import br.com.opin.mopclient.gateway.application.service.ResponseBuilder;

/**
 * User-facing copy when the payload is stored in the client retry queue because MOP is unavailable.
 * Returned together with HTTP 202 Accepted (delivery deferred), distinct from HTTP 200 OK
 * (synchronous delivery).
 */
public final class ClientRetryUserMessages {

    public static final String SUCCESS_AFTER_ENQUEUE = ResponseBuilder.API_ACCEPTED_BODY_MESSAGE;

    private ClientRetryUserMessages() {
    }
}
