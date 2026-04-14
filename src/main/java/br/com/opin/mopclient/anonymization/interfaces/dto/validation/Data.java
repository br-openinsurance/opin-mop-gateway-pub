package br.com.opin.mopclient.anonymization.interfaces.dto.validation;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Data {

    private List<Validation> validations;
    private String message;
    private String statusCode;
}
