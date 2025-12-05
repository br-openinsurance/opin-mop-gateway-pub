package br.com.opin.mopclient.gateway.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErroDTO {
    private String failedMessage;
    private String reason;
    private Throwable cause;
}
