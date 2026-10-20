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

> **Último scan Trivy (2026-10-07):** Alpine **3.24.2** = **0**. JAR = **1** (`CVE-2026-47884` em `spring-webmvc` 6.2.19). Exploração exige `XsltView` + mapeamento `/**` com renderização de view sem nome explícito — **não se aplica** a este REST. Ver [`CVE-2026-47884.md`](CVE-2026-47884.md) e VEX [`vex-CVE-2026-47884.json`](vex-CVE-2026-47884.json). Evidência: [`image-20261007-000000.png`](image-20261007-000000.png).
>
> **MAPFRE / Trivy (2026-10):** remediação — Jackson 2 **2.21.7**, Jackson 3 **3.1.7**, Tomcat **11.0.25**, RabbitMQ AMQP **5.37.0**, Spring Boot **4.1.1** / Spring Framework **7.0.9**. Actuator sem `*`; respostas de erro sem `Throwable#getMessage()`. `spring-boot-devtools` não está no projeto.
>
> **Dependabot (2026-10):** tratados na **1.0.8** — **#38** Spring Framework Improper Path Limitation in XsltView (Critical, **CVE-2026-47884**) → Spring Framework **7.0.9** + Boot **4.1.1**. **#40** Malformed UTF-8 in shortstr; **#39** plaintext broker credentials in `ConnectionFactoryConfigurator.load()`; **#41** JSONReader JSON-RPC DoS (todos Moderate, `amqp-client`) → **5.37.0** (os três; ver [`amqp-client-dependabot.md`](amqp-client-dependabot.md)).
>
> **Tenable (2026-06-26):** achados em dependências Java eram severidade **Low** (ex.: Spring `spring-webmvc` 6.2.11) — **fora** do escopo de remediação acima.
>
> **Alpine (histórico 2026-06):** **Critical** e **High** em `sqlite-libs` e `libgcrypt` na 3.23 — **remediado** na 3.24. Detalhes: [ALPINE-2026.md](ALPINE-2026.md).

## Inventário de varreduras

| Ferramenta | Data da execução | Artefatos | Observações |
|------------|------------------|-----------|-------------|
| **Trivy** (screenshot — **último scan**) | **2026-10-07** | [`image-20261007-000000.png`](image-20261007-000000.png) · [`CVE-2026-47884.md`](CVE-2026-47884.md) · [`vex-CVE-2026-47884.json`](vex-CVE-2026-47884.json) | Alpine **0**; JAR **1** CVE-2026-47884 — **VEX not_affected** |
| **Trivy** (screenshot) | **2026-10-06** 15:29 | [`image-20261006-152900.png`](image-20261006-152900.png) | CI `develop` com Spring 7.0.9 (quebrava o boot): Alpine **0**, JAR **0** |
| **Trivy** (imagem container + JAR) | **2026-06-26** | [`trivy.txt`](trivy.txt) | Export anterior: Alpine 3.23.5 — ver [ALPINE-2026.md](ALPINE-2026.md) |
| **Tenable** (SCA) | **2026-06-26** 11:01 | [`Vulnerabilities_All_2026-06-26-11_01.csv`](Vulnerabilities_All_2026-06-26-11_01.csv) | Export completo de vulnerabilidades |
| **Tenable** (SCA, filtrado) | **2026-06-26** 11:01 | [`Software_Filtered_2026-06-26-11_01.csv`](Software_Filtered_2026-06-26-11_01.csv) | Visão filtrada por software |
| **Tenable** (screenshot) | **2026-06-26** 13:58 | [`image-20260626-135821.png`](image-20260626-135821.png) | Evidência visual |

## Como adicionar novas varreduras

1. Armazene exports CSV, JSON ou TXT nesta pasta com o padrão `Ferramenta_Descricao_AAAA-MM-DD-HH_MM.ext`.
2. Screenshots na mesma pasta, com a data da varredura no nome do arquivo (ex.: `image-20260626-135821.png`).
3. Atualize a tabela acima e a seção **Varreduras de vulnerabilidade** no [`../../wiki.md`](../../wiki.md#varreduras-de-vulnerabilidade).
