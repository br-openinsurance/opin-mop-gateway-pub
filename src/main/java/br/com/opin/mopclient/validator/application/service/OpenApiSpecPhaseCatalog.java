package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps {@code swagger/current/} spec files and MOP paths to Open Insurance phases,
 * aligned with the official wiki.
 */
final class OpenApiSpecPhaseCatalog {

    private static final Map<String, OpenInsurancePhase> BY_FILE = buildFileIndex();

    private OpenApiSpecPhaseCatalog() {
    }

    static OpenInsurancePhase phaseForFile(String fileName) {
        if (fileName == null) {
            return OpenInsurancePhase.INTERNAL;
        }
        return BY_FILE.getOrDefault(fileName, phaseForFileNameHeuristic(fileName));
    }

    static boolean excludedFromOpenInsuranceValidation(String fileName) {
        return phaseForFile(fileName) == OpenInsurancePhase.INTERNAL;
    }

    static OpenInsurancePhase phaseForMopPath(String mopPath) {
        String normalized = OpenApiPathMatcher.normalizePath(mopPath);
        if (!normalized.startsWith(OpenApiPathMatcher.OPEN_INSURANCE_PREFIX)) {
            return OpenInsurancePhase.INTERNAL;
        }
        if (normalized.startsWith("/open-insurance/consents/")) {
            return OpenInsurancePhase.FASE_2_AND_3;
        }
        if (normalized.startsWith("/open-insurance/resources/")) {
            return OpenInsurancePhase.FASE_2_AND_3;
        }
        if (normalized.startsWith("/open-insurance/products-services/")
                || normalized.startsWith("/open-insurance/channels/")) {
            return OpenInsurancePhase.FASE_1;
        }
        if (normalized.startsWith("/open-insurance/customers/")) {
            return OpenInsurancePhase.FASE_2;
        }
        if (isPhase3PathPrefix(normalized)) {
            return OpenInsurancePhase.FASE_3;
        }
        if (isPhase2InsurancePath(normalized)) {
            return OpenInsurancePhase.FASE_2;
        }
        return OpenInsurancePhase.INTERNAL;
    }

    static Optional<String> wikiUrlFor(OpenInsurancePhase phase) {
        return switch (phase) {
            case FASE_1 -> Optional.of(
                    "https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/753678/Fase+1+-+Dados+Abertos");
            case FASE_2 -> Optional.of(
                    "https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/786475/Fase+2+-+Dados+Relacionados+Movimenta+es");
            case FASE_3 -> Optional.of(
                    "https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/4391146/Fase+3+-+Servi+os+de+Inicia+o+de+Movimenta+o");
            case FASE_2_AND_3 -> Optional.of(
                    "https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/786475/Fase+2+-+Dados+Relacionados+Movimenta+es");
            case INTERNAL -> Optional.empty();
        };
    }

    private static boolean isPhase3PathPrefix(String normalized) {
        return normalized.startsWith("/open-insurance/claim-notification/")
                || normalized.startsWith("/open-insurance/endorsement/")
                || normalized.startsWith("/open-insurance/quote-")
                || normalized.startsWith("/open-insurance/contract-life-pension/")
                || normalized.startsWith("/open-insurance/withdrawal/")
                || normalized.startsWith("/open-insurance/dynamic-fields/")
                || normalized.startsWith("/open-insurance/webhook/");
    }

    private static boolean isPhase2InsurancePath(String normalized) {
        return normalized.startsWith("/open-insurance/insurance-");
    }

    private static OpenInsurancePhase phaseForFileNameHeuristic(String fileName) {
        if (fileName.startsWith("quote-") || fileName.equals("claim-notification.yaml")
                || fileName.equals("endorsement.yaml") || fileName.equals("dynamic-fields.yaml")
                || fileName.equals("webhook.yaml")) {
            return OpenInsurancePhase.FASE_3;
        }
        if (fileName.startsWith("insurance-") || fileName.equals("customers.yaml")) {
            return OpenInsurancePhase.FASE_2;
        }
        if (fileName.startsWith("consents_") || fileName.startsWith("resources_")) {
            return OpenInsurancePhase.FASE_2_AND_3;
        }
        return OpenInsurancePhase.FASE_1;
    }

    private static Map<String, OpenInsurancePhase> buildFileIndex() {
        Map<String, OpenInsurancePhase> index = new HashMap<>();

        // Fase 1 — https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/753678
        registerFase1(index,
                "data_channels.yaml",
                "intermediary.yaml",
                "referenced-network.yaml",
                "assistance-general-assets.yaml",
                "auto-extended-warranty.yaml",
                "auto-insurance.yaml",
                "business.yaml",
                "capitalization-title.yaml",
                "condominium.yaml",
                "cyber-risk.yaml",
                "directors-officers-liability.yaml",
                "domestic-credit.yaml",
                "engineering.yaml",
                "environmental-liability.yaml",
                "equipment-breakdown.yaml",
                "errors-omissions-liability.yaml",
                "export-credit.yaml",
                "extended-warranty.yaml",
                "financial-risk.yaml",
                "general-liability.yaml",
                "global-banking.yaml",
                "home-insurance.yaml",
                "housing.yaml",
                "life-pension.yaml",
                "lost-profit.yaml",
                "named-operational-risks.yaml",
                "pension-plan.yaml",
                "person.yaml",
                "private-guarantee.yaml",
                "public-guarantee.yaml",
                "rent-guarantee.yaml",
                "rural.yaml",
                "stop-loss.yaml",
                "transport.yaml");

        // Fase 2 — https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/786475
        registerFase2(index,
                "customers.yaml",
                "insurance-acceptance-and-branches-abroad.yaml",
                "insurance-auto.yaml",
                "insurance-capitalization-title.yaml",
                "insurance-financial-assistance.yaml",
                "insurance-financial-risk.yaml",
                "insurance-housing.yaml",
                "insurance-life-pension.yaml",
                "insurance-patrimonial.yaml",
                "insurance-pension-plan.yaml",
                "insurance-person.yaml",
                "insurance-responsibility.yaml",
                "insurance-rural.yaml",
                "insurance-transport.yaml");

        // Fases 2 e 3 (transversal)
        register(index, OpenInsurancePhase.FASE_2_AND_3,
                "consents_v2.yaml",
                "consents_v3.yaml",
                "resources_v2.yaml",
                "resources_v3.yaml");

        // Fase 3 — https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/4391146
        registerFase3(index,
                "claim-notification.yaml",
                "dynamic-fields.yaml",
                "endorsement.yaml",
                "quote-acceptance-and-branches-abroad.yaml",
                "quote-auto.yaml",
                "quote-capitalization-title.yaml",
                "quote-financial-risk.yaml",
                "quote-housing.yaml",
                "quote-life-pension.yaml",
                "quote-life-pension-withdrawal.yaml",
                "quote-patrimonial.yaml",
                "quote-person.yaml",
                "quote-responsibility.yaml",
                "quote-rural.yaml",
                "quote-transport.yaml",
                "webhook.yaml");

        // Infraestrutura / fora das fases MOP (não indexados para validação Open Insurance)
        register(index, OpenInsurancePhase.INTERNAL,
                "consent-funnel-ingestion.yaml",
                "discovery.yaml",
                "admin_metrics.yaml");

        return Map.copyOf(index);
    }

    private static void registerFase1(Map<String, OpenInsurancePhase> index, String... files) {
        register(index, OpenInsurancePhase.FASE_1, files);
    }

    private static void registerFase2(Map<String, OpenInsurancePhase> index, String... files) {
        register(index, OpenInsurancePhase.FASE_2, files);
    }

    private static void registerFase3(Map<String, OpenInsurancePhase> index, String... files) {
        register(index, OpenInsurancePhase.FASE_3, files);
    }

    private static void register(Map<String, OpenInsurancePhase> index, OpenInsurancePhase phase, String... files) {
        for (String file : files) {
            index.put(file, phase);
        }
    }
}
