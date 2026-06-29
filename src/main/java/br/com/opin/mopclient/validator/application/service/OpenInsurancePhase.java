package br.com.opin.mopclient.validator.application.service;

/**
 * Open Insurance implementation phase for specs under {@code swagger/current/}.
 *
 * @see <a href="https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/753678">Fase 1 - Dados Abertos</a>
 * @see <a href="https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/786475">Fase 2 - Dados Relacionados à Movimentações</a>
 * @see <a href="https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/4391146">Fase 3 - Serviços de Iniciação de Movimentação</a>
 */
public enum OpenInsurancePhase {

    /** Fase 1 — Dados abertos (canais + products-services). */
    FASE_1("Fase 1 - Dados Abertos"),

    /** Fase 2 — Dados cadastrais e de contratos (compartilhamento com consentimento). */
    FASE_2("Fase 2 - Dados Relacionados à Movimentações"),

    /** Fase 3 — Cotação, contratação, sinistro, endosso e webhooks. */
    FASE_3("Fase 3 - Serviços de Iniciação de Movimentação"),

    /** APIs transversais às fases 2 e 3 (consentimento e resources). */
    FASE_2_AND_3("Fases 2 e 3 - Consents / Resources"),

    /** Specs de infraestrutura (ingestão PCM, discovery, admin) — fora do fluxo MOP de validação. */
    INTERNAL("Infraestrutura / PCM / Diretório");

    private final String label;

    OpenInsurancePhase(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
