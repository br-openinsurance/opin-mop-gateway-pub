package br.com.opin.mopclient.gateway.interfaces.constants;

public final class HttpHeaderConstants {

    /** Chave para cabeçalho de operação: GET, POST, PUT, DELETE */
    public static final String OPERATION = "operation";

    /** Chave para cabeçalho de origem. */
    public static final String ORIGIN = "origin";

    /** Chave para cabeçalho de destino. */
    public static final String DESTINATION = "destination";

    /** Chave para cabeçalho de caminho. */
    public static final String PATH = "path";

    /** Chave para cabeçalho de headers adicionais. */
    public static final String HEADERS = "headers";

    public static final String RESPONSE = "response";

    public static final String CORRELATIONID ="correlationID";

    public static final String USERID ="userID";

    public static final String TIMESTAMP ="timestamp";

    /** Chave para cabeçalho de modo de aplicação: TRANSMITTER ou RECEIVER */
    public static final String APPLICATION_MODE = "applicationMode";

}
