package br.com.opin.mopclient.security;

/**
 * Opções pontuais da assinatura JWT de saída (por thread / requisição).
 */
public final class OutboundSigningOptions {

    private static final ThreadLocal<Boolean> OMIT_IAT_EXP = new ThreadLocal<>();

    private OutboundSigningOptions() {
    }

    /**
     * Executa o envio do funil sem claims JWT {@code iat} e {@code exp}.
     */
    public static void runOmittingRegisteredTimeClaims(Runnable action) {
        OMIT_IAT_EXP.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            OMIT_IAT_EXP.remove();
        }
    }

    public static boolean omitRegisteredTimeClaims() {
        return Boolean.TRUE.equals(OMIT_IAT_EXP.get());
    }
}
