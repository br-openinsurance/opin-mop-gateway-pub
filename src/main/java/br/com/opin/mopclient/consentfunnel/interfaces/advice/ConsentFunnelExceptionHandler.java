package br.com.opin.mopclient.consentfunnel.interfaces.advice;

import br.com.opin.mopclient.consentfunnel.application.mapper.ConsentFunnelViolationMapper;
import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelValidationErrorResponseDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelViolationDTO;
import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tratamento centralizado de erros do bounded context {@code consentfunnel}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = br.com.opin.mopclient.consentfunnel.interfaces.controller.ConsentFunnelController.class)
public class ConsentFunnelExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentFunnelExceptionHandler.class);
    private static final String BEAN_VALIDATION_CODE = "400";
    private static final String ERROR_STATUS = "ERROR";

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ConsentFunnelValidationErrorResponseDTO> handleUnreadableBody(
            HttpMessageNotReadableException ex) {
        ConsentFunnelValidationException funnel = findFunnelValidation(ex);
        if (funnel != null) {
            return handleFunnelValidation(funnel);
        }
        LOGGER.warn("Consent funnel JSON is unreadable");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorResponse(List.of(
                ConsentFunnelViolationDTO.builder()
                        .violation("Invalid or unreadable JSON payload")
                        .code(BEAN_VALIDATION_CODE)
                        .severity("ERROR")
                        .attribute("body")
                        .build())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ConsentFunnelValidationErrorResponseDTO> handleBeanValidation(
            MethodArgumentNotValidException ex) {
        List<ConsentFunnelViolationDTO> violations = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            violations.add(ConsentFunnelViolationDTO.builder()
                    .violation(fieldError.getDefaultMessage())
                    .code(BEAN_VALIDATION_CODE)
                    .severity("ERROR")
                    .attribute(fieldError.getField())
                    .build());
        }
        LOGGER.warn("Consent funnel bean validation failed | violations={}", violations.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorResponse(violations));
    }

    @ExceptionHandler(ConsentFunnelValidationException.class)
    public ResponseEntity<ConsentFunnelValidationErrorResponseDTO> handleFunnelValidation(
            ConsentFunnelValidationException ex) {
        List<ConsentFunnelViolationDTO> violations = ex.getViolations().stream()
                .map(ConsentFunnelViolationMapper::toDto)
                .toList();
        LOGGER.warn("Consent funnel validation failed | message={} | violations={}", ex.getMessage(), violations.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorResponse(violations));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Map<String, String>> handleMetricsDeliveryFailure(ErrorResponseException ex) {
        LOGGER.warn("Consent funnel metrics delivery failed | error={} | details={}", ex.getError(), ex.getDetails());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "Metrics delivery failed");
        body.put("details", "The consent funnel event could not be delivered.");
        body.put("timestamp", ex.getTimestamp());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    private static ConsentFunnelValidationException findFunnelValidation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConsentFunnelValidationException funnel) {
                return funnel;
            }
            current = current.getCause();
        }
        return null;
    }

    private static ConsentFunnelValidationErrorResponseDTO buildErrorResponse(
            List<ConsentFunnelViolationDTO> violations) {
        return ConsentFunnelValidationErrorResponseDTO.builder()
                .status(ERROR_STATUS)
                .total(violations.size())
                .pending(violations)
                .build();
    }
}
