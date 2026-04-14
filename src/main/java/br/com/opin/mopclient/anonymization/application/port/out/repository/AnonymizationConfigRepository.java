package br.com.opin.mopclient.anonymization.application.port.out.repository;

import br.com.opin.mopclient.anonymization.interfaces.dto.AnonymizationConfigDTO;

/**
 * Port for retrieving anonymization configuration.
 */
public interface AnonymizationConfigRepository {

    /**
     * Retrieves anonymization configuration.
     *
     * @return AnonymizationConfigDTO with anonymized and exposed fields configuration.
     */
    AnonymizationConfigDTO findConfiguration();
}
