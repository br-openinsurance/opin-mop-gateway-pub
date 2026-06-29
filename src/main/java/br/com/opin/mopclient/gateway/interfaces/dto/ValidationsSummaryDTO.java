package br.com.opin.mopclient.gateway.interfaces.dto;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import br.com.opin.mopclient.gateway.application.service.ValidationExecutionStatusResolver;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAPI validation findings grouped for gateway API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "total", "pending"})
public class ValidationsSummaryDTO {

    @JsonProperty("status")
    private String status;

    @JsonProperty("total")
    private int total;

    @JsonProperty("pending")
    private List<Validation> pending;

    public static ValidationsSummaryDTO from(List<Validation> validations) {
        List<Validation> pending = ValidationExecutionStatusResolver.preserveValidations(validations);
        return ValidationsSummaryDTO.builder()
                .status(ValidationExecutionStatusResolver.resolveStatus(pending))
                .total(pending.size())
                .pending(pending)
                .build();
    }
}
