package br.com.opin.mopclient.anonymization.interfaces.dto.validation;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ValidationResponseDTO {

    private Data data;
}
