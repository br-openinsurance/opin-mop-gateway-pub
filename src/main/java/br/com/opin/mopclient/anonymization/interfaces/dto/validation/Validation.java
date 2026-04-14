package br.com.opin.mopclient.anonymization.interfaces.dto.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Validation {

    private String violation;
    private int code;
    private String severity;
    private String attribute;
}
