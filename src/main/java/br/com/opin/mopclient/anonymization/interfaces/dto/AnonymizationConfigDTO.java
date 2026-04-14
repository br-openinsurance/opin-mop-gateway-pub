package br.com.opin.mopclient.anonymization.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Data Transfer Object for anonymization configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymizationConfigDTO {

    /**
     * Set of field names that should be anonymized (encrypted).
     */
    private Set<String> anonymized;

    /**
     * Set of field names that should be exposed (kept as-is, not anonymized).
     */
    private Set<String> exposed;
}
