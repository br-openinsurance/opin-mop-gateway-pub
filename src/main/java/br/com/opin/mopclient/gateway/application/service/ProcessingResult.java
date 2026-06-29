package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import br.com.opin.mopclient.gateway.interfaces.dto.ServerResponseDTO;

import java.util.List;

/**
 * Result of synchronous request processing through the orchestrator.
 */
public record ProcessingResult(
        String wrappedJson,
        List<Validation> validations,
        ServerResponseDTO serverResponse) {
}
