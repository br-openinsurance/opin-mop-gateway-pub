# MOP Client — Wiki técnico

Visão técnica do **MOP Client** no estado atual do repositório: uma aplicação Spring Boot que expõe HTTP, executa o pipeline interno de processamento e chama o **servidor MOP**; em falhas transitórias, usa **RabbitMQ** apenas como **fila de retry** e **Resilience4j** para circuitos nos clientes HTTP downstream.

> [!NOTE]
> **Versão estável de produção:** a branch **`main`** publica releases versionadas no GHCR — linha oficial para ambientes produtivos. **A versão mais recente em produção é sempre identificada por tag semver** (ex.: **`v1.0.5`**), e não pelo nome da branch. A branch **`develop`** permanece agora somente dedicada a homologação/sandbox (tag `develop`).
>
> ```bash
> docker pull ghcr.io/br-openinsurance/opin-mop-gateway-pub/open-insurance-mop-gateway:v1.0.5
> ```

## Índice

1. [Visão geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Fluxo de processamento](#fluxo-de-processamento)
4. [Resiliência e retry](#resiliência-e-retry)
5. [API HTTP](#api-http)
6. [Instalação (Helm)](#instalação-helm)
7. [Configuração](#configuração)
8. [Rastreamento](#rastreamento)
9. [Monitoramento](#monitoramento)
10. [Desenvolvimento](#desenvolvimento)
11. [Solução de problemas](#solução-de-problemas)
12. [Varreduras de vulnerabilidade](#varreduras-de-vulnerabilidade)
13. [Referências](#referências)

---

## Visão geral

| Componente | Função |
|------------|--------|
| `AnonymizerController` | `POST` em `/data` (sob o context path configurado). |
| Pipeline interno | Processamento síncrono até POST ao MOP (detalhes no código em `ProcessingOrchestratorService` e módulos associados). |
| `ClientRetryEnqueueService` / fila AMQP | Persistência de trabalho quando o MOP não está acessível; *replay* posterior. |
| `MopServerAvailabilityProbe` | Sondas periódicas de disponibilidade. |
| Circuit breakers | Instâncias `mopAnonymizationConfig` e `mopProcessEndpoint` (Resilience4j). |

Não há dependência de **outros repositórios** como serviços obrigatórios em tempo de execução: tudo roda neste deployável.

---

## Arquitetura

```
Cliente HTTP → Controller → Orquestração → Clientes HTTP (config + /process) [+ circuitos]
                    ↓ (se indisponível)
              Fila RabbitMQ (retry) → Replay scheduler
```

Cache (Caffeine) reduz chamadas repetidas a especificação e configurações.

---

## Fluxo de processamento

1. Validação de headers obrigatórios (`HeaderValidator`) — inclui path completo sob `/open-insurance/` e par `origin`/`httpType`.
2. Parse do corpo JSON (`JsonPayloadParser`).
3. Validação OpenAPI (`OpenApiValidationService`) com path MOP completo.
4. Execução do pipeline interno e envio ao MOP ou enfileiramento conforme política de resiliência.

Detalhes de estágios internos: código-fonte e comentários em `ProcessingOrchestratorService`.

---

## Resiliência e retry

- **Circuit breakers** protegem chamadas aos endpoints de configuração e de processamento.
- **Fila `mop.client.retry.queue`:** mensagens JSON com contexto para reenvio.
- **Replay:** `ClientRetryReplayService` / `ClientRetryReplayScheduler` — intervalos em `mop.client.retry.replay.*`.

Documentação dedicada: [docs/REPROCESSAMENTO.md](docs/REPROCESSAMENTO.md).

---

## API HTTP

- URL típica: `http(s)://{host}:{port}{server.servlet.context-path}/data` (padrão `…/v1/anonymize/data`).
- Headers obrigatórios: `X-Correlation-Id`, `origin` (`client`/`server`), `path`, `operation`, `httpType`.
- Header condicional: `statusCode` — opcional com `httpType=Request`; **obrigatório** com `httpType=Response` (100–599).
- Opcionais: `traceOrigin`, `clientSSId`, `serverASId`, `X-Mop-Reportid`.
- **`path`:** enviar o path **concreto** da transação (path MOP completo, começando com `/open-insurance/`; com URN/ID reais); não usar `{consentId}` nem só `/consents`. Ver [`docs/PATH_MOP_HEADER.md`](docs/PATH_MOP_HEADER.md).
- **`origin` + `httpType` + `statusCode`:** combinação fixa — `client`+`Request` (valida requestBody) ou `server`+`Response`+`statusCode` (valida response body). Ver tabela em [`README.md`](README.md#contrato-da-api) e [`docs/PATH_MOP_HEADER.md`](docs/PATH_MOP_HEADER.md).
- Resposta HTTP: `context`, `request` (com `path`, `operation`, `header`), `validations` (`status`, `total`, `pending`); `response` quando entrega síncrona ao MOP.

Contrato resumido: [README.md](README.md).

---

## Instalação (Helm)

Implantação em **Kubernetes** via **Helm Chart** (GHCR: `ghcr.io/br-openinsurance/mop-client-chart/mop-client`).

- Guia oficial: [INSTALA_MOP_CLIENT.md](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/feat/mop-client-install/docs/INSTALA_MOP_CLIENT.md) no repositório `opin-mop-gateway-pub` (branch `feat/mop-client-install`).
- Índice local: [docs/INSTALACAO.md](docs/INSTALACAO.md).

Imagem Docker de **produção** (GHCR, release **1.0.5**, branch `main`):

```bash
docker pull ghcr.io/br-openinsurance/opin-mop-gateway-pub/open-insurance-mop-gateway:v1.0.5
```

> Homologação/sandbox: tag `develop` — ver secção [Desenvolvimento](#desenvolvimento).

---

## Configuração

- `application.yml` — base, RabbitMQ, MOP, cache, Resilience4j, logging.
- `application-local.yml` — sobrescritas locais; ativar com `SPRING_PROFILES_ACTIVE=local`.

Variáveis: [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md).

### Endpoints do MOP Server

| Ambiente | POST `/process` | GET `anonymization-fields` |
|----------|-----------------|----------------------------|
| Sandbox | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process` | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent` |
| Produção | `https://mop-server-entrypoint.opinbrasil.com.br/process` | `https://mop-server-entrypoint.opinbrasil.com.br/anonymization-fields?schema=Consent` |

---

## Rastreamento

- **`X-Correlation-Id`:** obrigatório no contrato HTTP.
- Logs estruturados e MOP report ID onde aplicável no código.

---

## Monitoramento

- Spring Boot Actuator + Micrometer/Prometheus (conforme dependências do `pom.xml`).
- Health: `GET {context-path}/actuator/health`.

---

## Desenvolvimento

Imagem publicada (branch `develop`):

```bash
docker pull ghcr.io/br-openinsurance/opin-mop-gateway-pub/open-insurance-mop-gateway:develop
```

Build e testes a partir do código-fonte:

```bash
mvn test
mvn clean package
```

---

## Solução de problemas

| Sintoma | Verificação |
|---------|-------------|
| Erro de conexão AMQP | Broker RabbitMQ ativo; credenciais e `RABBITMQ_*`. |
| Timeouts no MOP | URLs `EXTERNAL_*`, rede, circuit breakers abertos. |
| Mensagens na fila de retry | Logs `[MOP replay]` / `[MOP retry]`; parâmetros `mop.client.retry.*`. |

---

## Varreduras de vulnerabilidade

Relatórios e evidências em [`docs/vulnerabilities/`](docs/vulnerabilities/).

### Escopo de tratamento (severidade)

Somente vulnerabilidades classificadas como **Critical**, **High** ou **Medium** entram no escopo de **remediação obrigatória** (acompanhamento, priorização e correção). Níveis **Low**, **Info** e demais podem constar nos relatórios, mas **não exigem** ticket de remediação nem bloqueio de release.

| Severidade | Tratada |
|------------|:-------:|
| Critical | Sim |
| High | Sim |
| Medium | Sim |
| Low | Não |
| Info / outros | Não |

Detalhes: [`docs/vulnerabilities/README.md`](docs/vulnerabilities/README.md#escopo-de-remediação-severidade) · achados Alpine: [`docs/vulnerabilities/ALPINE-2026.md`](docs/vulnerabilities/ALPINE-2026.md).

> **Último export Tenable (2026-06-26):** achados em dependências Java são **Low** (ex.: Spring `spring-webmvc` 6.2.11) — fora do escopo de remediação acima.
>
> **Alpine (imagem base):** CVE-2026-11822, CVE-2026-11824 (**Critical**, `sqlite-libs`) e CVE-2026-41989 (**High**, `libgcrypt`) — **no escopo**. Ver [ALPINE-2026.md](docs/vulnerabilities/ALPINE-2026.md).

| Ferramenta | Data da execução | Artefatos | Resumo |
|------------|------------------|-----------|--------|
| **Trivy** (imagem container + JAR) | **2026-06-26** | [`docs/vulnerabilities/trivy.txt`](docs/vulnerabilities/trivy.txt) | Imagem GHCR `develop` (Alpine 3.23.5): **Critical/High** em pacotes OS — [detalhes](docs/vulnerabilities/ALPINE-2026.md); JAR sem achados no export |
| **Tenable** (SCA) | **2026-06-26** 11:01 | [`docs/vulnerabilities/Vulnerabilities_All_2026-06-26-11_01.csv`](docs/vulnerabilities/Vulnerabilities_All_2026-06-26-11_01.csv) · [`docs/vulnerabilities/Software_Filtered_2026-06-26-11_01.csv`](docs/vulnerabilities/Software_Filtered_2026-06-26-11_01.csv) | Achados em dependências (ex.: Spring `spring-webmvc` 6.2.11); ver CSV para severidade e fix version |
| **Tenable** (screenshot) | **2026-06-26** 13:58 | [`docs/vulnerabilities/image-20260626-135821.png`](docs/vulnerabilities/image-20260626-135821.png) | Evidência visual da varredura |

---

## Referências

- [README.md](README.md)
- [docs/INSTALACAO.md](docs/INSTALACAO.md) · [Helm (guia oficial)](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/feat/mop-client-install/docs/INSTALA_MOP_CLIENT.md)
- [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md)
- [docs/REPROCESSAMENTO.md](docs/REPROCESSAMENTO.md)
- [docs/vulnerabilities/](docs/vulnerabilities/) — artefatos de varredura (Trivy, Tenable)
- [docs/vulnerabilities/ALPINE-2026.md](docs/vulnerabilities/ALPINE-2026.md) — CVEs Critical/High na imagem Alpine
- [Open Insurance Brasil](https://www.gov.br/susep/pt-br/assuntos/open-insurance)

**Última revisão:** alinhada ao serviço unificado com fila de retry e circuitos HTTP.
