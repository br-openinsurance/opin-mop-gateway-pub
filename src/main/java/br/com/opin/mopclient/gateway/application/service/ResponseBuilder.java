package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestSummaryDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.ResponseContextDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.ServerResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.ValidationsSummaryDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Service for building HTTP responses.
 * <p>
 * Encapsulates the logic for creating structured success and error responses.
 */
@Service
public class ResponseBuilder {

    /**
     * Body message returned when the payload was synchronously delivered to the MOP server.
     */
    public static final String API_SUCCESS_BODY_MESSAGE =
            "Request processed successfully. Your data has been received and forwarded to the server.";

    /**
     * Body message returned when the payload could not be delivered synchronously and was stored
     * in the client retry queue for later delivery (HTTP 202 Accepted).
     */
    public static final String API_ACCEPTED_BODY_MESSAGE =
            "Request accepted and queued for later delivery to the server (MOP unavailable).";

    private static final String SUCCESS_MESSAGE = API_SUCCESS_BODY_MESSAGE;

    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation) {
        return buildSuccessResponse(correlationId, timestamp, clientSSId, serverASId, path, operation, null, null, null);
    }

    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation,
                                                               List<Validation> validations) {
        return buildSuccessResponse(correlationId, timestamp, clientSSId, serverASId, path, operation, null, validations, null);
    }

    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation,
                                                               List<Validation> validations,
                                                               ServerResponseDTO serverResponse) {
        return buildSuccessResponse(correlationId, timestamp, clientSSId, serverASId, path, operation, null, validations, serverResponse);
    }

    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation,
                                                               String message,
                                                               List<Validation> validations,
                                                               ServerResponseDTO serverResponse) {
        List<Validation> preservedValidations = ValidationExecutionStatusResolver.preserveValidations(validations);
        return buildResponse(HttpStatus.OK,
                message != null ? message : SUCCESS_MESSAGE,
                correlationId, timestamp, clientSSId, serverASId, path, operation, preservedValidations, serverResponse);
    }

    public ResponseEntity<ApiResponseDTO> buildAcceptedResponse(String correlationId, String timestamp,
                                                                String clientSSId, String serverASId,
                                                                String path, String operation,
                                                                String message) {
        return buildResponse(HttpStatus.ACCEPTED,
                message != null ? message : API_ACCEPTED_BODY_MESSAGE,
                correlationId, timestamp, clientSSId, serverASId, path, operation, null, null);
    }

    private ResponseEntity<ApiResponseDTO> buildResponse(HttpStatus httpStatus, String message,
                                                         String correlationId, String timestamp,
                                                         String clientSSId, String serverASId,
                                                         String path, String operation,
                                                         List<Validation> validations,
                                                         ServerResponseDTO mopResponse) {
        List<Validation> responseValidations = validations != null ? validations : Collections.emptyList();
        ApiResponseDTO response = ApiResponseDTO.builder()
                .message(message)
                .timestamp(timestamp)
                .context(ResponseContextDTO.builder()
                        .correlationId(correlationId)
                        .clientSSId(clientSSId)
                        .serverASId(serverASId)
                        .build())
                .request(RequestSummaryDTO.builder().path(path).operation(operation).build())
                .validations(ValidationsSummaryDTO.from(responseValidations))
                .response(mopResponse)
                .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    public ResponseEntity<ApiResponseDTO> buildErrorResponse(HttpStatus status, String error, String details) {
        ApiResponseDTO response = ApiResponseDTO.builder()
                .error(error)
                .details(details)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.status(status.value()).body(response);
    }
}
