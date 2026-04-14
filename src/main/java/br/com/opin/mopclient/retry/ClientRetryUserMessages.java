package br.com.opin.mopclient.retry;

import br.com.opin.mopclient.gateway.application.service.ResponseBuilder;

/**
 * User-facing copy when the payload is stored in the client retry queue.
 * The HTTP body matches the normal success message; unavailability is logged only.
 */
public final class ClientRetryUserMessages {

    public static final String SUCCESS_AFTER_ENQUEUE = ResponseBuilder.API_SUCCESS_BODY_MESSAGE;

    private ClientRetryUserMessages() {
    }
}
