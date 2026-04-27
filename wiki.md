# MOP Client — Wiki técnico

Visão técnica do **MOP Client** no estado atual do repositório: uma aplicação Spring Boot que expõe HTTP, executa o pipeline interno de processamento e chama o **servidor MOP**; em falhas transitórias, usa **RabbitMQ** apenas como **fila de retry** e **Resilience4j** para circuitos nos clientes HTTP downstream.

## Índice

1. [Visão geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Fluxo de processamento](#fluxo-de-processamento)
4. [Resiliência e retry](#resiliência-e-retry)
5. [API HTTP](#api-http)
6. [Configuração](#configuração)
7. [Rastreamento](#rastreamento)
8. [Monitoramento](#monitoramento)
9. [Desenvolvimento](#desenvolvimento)
10. [Troubleshooting](#troubleshooting)
11. [Referências](#referências)

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

1. Validação de headers obrigatórios (`HeaderValidator`).
2. Parse do corpo JSON (`JsonPayloadParser`).
3. Execução do pipeline interno e envio ao MOP ou enfileiramento conforme política de resiliência.

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
- Headers obrigatórios incluem `X-Correlation-Id`, `origin` (`client`/`server`), `path`, `operation`, `step`, `dataEventoStep`, `clientSSId`, `serverASId`.

Contrato resumido: [README.md](README.md).

---

## Configuração

- `application.yml` — base, RabbitMQ, MOP, cache, Resilience4j, logging.
- `application-local.yml` — sobrescritas locais; ativar com `SPRING_PROFILES_ACTIVE=local`.

Variáveis: [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md).

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

```bash
mvn test
mvn clean package
```

---

## Troubleshooting

| Sintoma | Verificação |
|---------|-------------|
| Erro de conexão AMQP | Broker RabbitMQ ativo; credenciais e `RABBITMQ_*`. |
| Timeouts no MOP | URLs `EXTERNAL_*`, rede, circuit breakers abertos. |
| Mensagens na fila de retry | Logs `[MOP replay]` / `[MOP retry]`; parâmetros `mop.client.retry.*`. |

---

## Referências

- [README.md](README.md)
- [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md)
- [docs/REPROCESSAMENTO.md](docs/REPROCESSAMENTO.md)
- [Open Insurance Brasil](https://www.gov.br/susep/pt-br/assuntos/open-insurance)

**Última revisão:** alinhada ao serviço unificado com fila de retry e circuitos HTTP.
