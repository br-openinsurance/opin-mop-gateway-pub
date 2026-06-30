# Vulnerabilidades

Varreduras de segurança, achados e notas de remediação do **opin-mop-client-gateway**.

## Escopo de remediação (severidade)

Somente achados com os níveis de severidade abaixo estão **no escopo de remediação** (acompanhados, priorizados e corrigidos):

| Severidade | No escopo |
|------------|:---------:|
| **Critical** | Sim |
| **High** | Sim |
| **Medium** | Sim |
| **Low** | Não |
| **Info** / outros | Não |

Achados fora desse escopo podem constar nos exports das varreduras por transparência, mas **não exigem** ticket de remediação nem bloqueio de release.

> **Último export Tenable (2026-06-26):** os itens reportados são severidade **Low** (ex.: Spring `spring-webmvc` 6.2.11) — documentados no CSV, **fora** do escopo de remediação acima.

## Inventário de varreduras

| Ferramenta | Data da execução | Artefatos | Observações |
|------------|------------------|-----------|-------------|
| **Trivy** (imagem container) | **2026-04-27** | [`../images/trivy-scan-2026-04-27.png`](../images/trivy-scan-2026-04-27.png) | Alvo: imagem `open-insurance-mop-gateway` (Alpine 3.23.5) + `mop-client-gateway.jar` — 0 vulnerabilidades |
| **Trivy** (export texto) | — | [`trivy.txt`](trivy.txt) | Resumo do relatório CLI; 0 vulnerabilidades nos alvos varridos |
| **Tenable** (SCA) | **2026-06-26** 11:01 | [`Vulnerabilities_All_2026-06-26-11_01.csv`](Vulnerabilities_All_2026-06-26-11_01.csv) | Export completo de vulnerabilidades |
| **Tenable** (SCA, filtrado) | **2026-06-26** 11:01 | [`Software_Filtered_2026-06-26-11_01.csv`](Software_Filtered_2026-06-26-11_01.csv) | Visão filtrada por software |
| **Tenable** (screenshot) | **2026-04-27** | [`../images/tenable-scan-2026-04-27.png`](../images/tenable-scan-2026-04-27.png) | Evidência visual |

## Como adicionar novas varreduras

1. Armazene exports CSV, JSON ou TXT nesta pasta com o padrão `Ferramenta_Descricao_AAAA-MM-DD-HH_MM.ext`.
2. Opcionalmente, adicione screenshots em [`../images/`](../images/) com a data da varredura no nome do arquivo.
3. Atualize a tabela acima e a seção **Varreduras de vulnerabilidade** no [`../../wiki.md`](../../wiki.md#varreduras-de-vulnerabilidade).

## Artefatos relacionados

- Screenshots: [`../images/`](../images/)
