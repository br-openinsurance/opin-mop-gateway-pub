# Variáveis de ambiente e propriedades

Referência completa das variáveis e propriedades Spring Boot do **MOP Client**, conforme `src/main/resources/application.yml` e `src/main/resources/application-local.yml`.

> **Convenção:** o Spring Boot aceita nomes em **MAIÚSCULAS** com `_` no lugar de `.` (relaxed binding). Ex.: `external.request.url` ⇔ `EXTERNAL_REQUEST_URL`.

> **Como ler esta tabela:**
> - **Padrão (base)** = valor em `application.yml` (profile `default`).
> - **Padrão (local)** = valor em `application-local.yml` quando diferente do base.
> - **Sem default** = a variável **deve** ser definida; senão a aplicação falha no startup.

---

## Sumário

1. [Perfil Spring](#perfil-spring)
2. [Servidor HTTP](#servidor-http)
3. [APIs externas (MOP)](#apis-externas-mop)
4. [Assinatura JWS (obrigatório)](#assinatura-jws-obrigatório)
5. [RabbitMQ](#rabbitmq)
6. [Retry do cliente e disponibilidade do MOP](#retry-do-cliente-e-disponibilidade-do-mop)
7. [Cache](#cache)
8. [Actuator](#actuator)
9. [Logging](#logging)
10. [Referência rápida — sandbox](#referência-rápida--sandbox)

---

## Perfil Spring

| Propriedade | Variável de ambiente | Padrão (base) | Descrição |
|---|---|---|---|
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `default` | Perfis ativos. Em dev use `local`; em prod, deixe vazio ou use perfis próprios (`dev`, `homolog`, `prod`). |

---

## Servidor HTTP

| Propriedade | Variável | Padrão | Descrição |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` (hard-coded no YAML) | Porta HTTP. |
| `server.servlet.context-path` | `SERVER_CONTEXT_PATH` | `/v1/anonymize` (hard-coded no YAML) | Prefixo dos endpoints. Ex.: `POST /v1/anonymize/data`, health em `/v1/anonymize/actuator/health`. |

> ⚠️ Hoje esses dois valores estão **hard-coded** em `application.yml` (sem placeholder). Para alterar, edite o YAML ou use `--server.port=...` / `SPRING_APPLICATION_JSON`. As env vars `SERVER_PORT`/`SERVER_CONTEXT_PATH` **não** têm efeito até que sejam adicionados placeholders no YAML.

---

## APIs externas (MOP)

### POST — envio do payload processado

| Propriedade | Variável | Padrão (base) | Descrição |
|---|---|---|---|
| `external.request.url` | `EXTERNAL_REQUEST_URL` | derivado de `external.request.host` + `external.request.path` | URL completa usada no envio ao MOP. **Vence as demais quando definida.** |
| `external.host` | `EXTERNAL_HOST` | **sem default** ⚠️ | Host base usado para compor `request.host` e `api.data-anonymization`. **Defina sempre** — a aplicação falha no boot se ausente. |
| `external.request.host` | `EXTERNAL_REQUEST_HOST` | `${external.host}` | Host complementar. |
| `external.request.path` | `EXTERNAL_REQUEST_PATH` | `/process` | Caminho. |
| `external.request.method` | `EXTERNAL_REQUEST_METHOD` | `POST` | Método HTTP. |
| `external.server.request.url` | — | derivado de `external.request.url` | **Legado.** Mantido por compatibilidade; será removido. |

#### Precedência efetiva da URL

```
1º) EXTERNAL_REQUEST_URL                       ← se definida, vence tudo
2º) EXTERNAL_REQUEST_HOST + EXTERNAL_REQUEST_PATH
3º) EXTERNAL_HOST          + /process
4º) external.server.request.url                ← LEGADO
```

### GET — regras de campos (configuração dinâmica de anonimização)

| Propriedade | Variável | Padrão |
|---|---|---|
| `external.api.data-anonymization` | `EXTERNAL_API_DATA_ANONYMIZATION` | `${external.host}/anonymization-fields?schema=Consent` |

> [!IMPORTANT]
> Em qualquer ambiente que não seja desenvolvimento local, defina `EXTERNAL_API_DATA_ANONYMIZATION` **explicitamente**. O default é apenas conveniência local.

---

## Assinatura JWS (obrigatório)

Configura a assinatura do payload final enviado ao MOP.

| Propriedade | Variável | Padrão | Descrição |
|---|---|---|---|
| `mop.payload-signing.enabled` | `MOP_PAYLOAD_SIGNING_ENABLED` | **sem default** ⚠️ | Liga/desliga a assinatura JWS. Em produção: `true`. **A ausência da variável faz a aplicação falhar no boot** com `Could not resolve placeholder`. |
| `mop.payload-signing.private-key-pem` | `JWS_PRIVATE_KEY` | sem default | Chave privada PKCS#8 em PEM. Pode ser multilinha ou com `\n` literais. |
| `mop.payload-signing.key-id` | `JWS_KID` | sem default | `kid` no header JWT — deve casar com chave publicada no JWKS do participante. **Não pode ficar em branco.** |
| `mop.payload-signing.org-id` | `JWS_ORG_ID` | sem default | `orgId` (UUID) nas claims do JWT e em `trace.OrgId`. **Não pode ficar em branco.** |

**Algoritmo:** `PS256` (RSA-PSS / SHA-256).

> [!CAUTION]
> Em produção, **não** passe `JWS_PRIVATE_KEY` como string nua de env var. Monte como secret via Kubernetes/Vault/AWS Secrets Manager e leia para a env apenas no entrypoint do container.

---

## RabbitMQ

O projeto inclui `spring-boot-starter-amqp`. O broker é usado **exclusivamente** para a fila de retry do cliente (`mop.client.retry.queue`) — não para um pipeline de filas. **RabbitMQ é obrigatório**: sem ele, a aplicação não sobe.

| Propriedade | Variável | Padrão |
|---|---|---|
| `spring.rabbitmq.host` | `RABBITMQ_VALIDATOR_HOST` | **sem default** ⚠️ |
| `spring.rabbitmq.port` | `RABBITMQ_VALIDATOR_PORT` | **sem default** ⚠️ |
| `spring.rabbitmq.username` | `RABBITMQ_USERNAME` | **sem default** ⚠️ |
| `spring.rabbitmq.password` | `RABBITMQ_PASSWORD` | **sem default** ⚠️ |

> Todas são **obrigatórias**: a aplicação falha no boot com `Could not resolve placeholder` se qualquer uma estiver ausente. Para dev local típico, exporte `RABBITMQ_VALIDATOR_HOST=localhost`, `_PORT=5672`, `RABBITMQ_USERNAME=guest`, `RABBITMQ_PASSWORD=guest`.

> O sufixo `_VALIDATOR_` é herdado do antigo serviço descontinuado (validator). Será renomeado para `RABBITMQ_HOST` / `RABBITMQ_PORT` em uma próxima versão, com alias mantido.

> No profile `local` ainda existem propriedades antigas de listener e nomes de filas (`RABBITMQ_VALIDATOR_QUEUE_NAME`, `RABBITMQ_OUTPUT_QUEUE_NAME`, `RABBITMQ_CONCURRENCY`). **Estão obsoletas para o fluxo atual** — só são consumidas por componentes legados. A fila ativa é definida em `mop.client.retry.queue`.

---

## Retry do cliente e disponibilidade do MOP

Prefixo: `mop.client.retry` e `mop.server.availability`.

| Propriedade | Variável | Padrão (base) | Padrão (local) | Descrição |
|---|---|---|---|---|
| `mop.client.retry.queue` | `MOP_CLIENT_RETRY_QUEUE` | `mop.client.retry.queue` | (igual) | Nome da fila AMQP. |
| `mop.client.retry.replay.enabled` | `MOP_CLIENT_RETRY_REPLAY_ENABLED` | `true` | (igual) | Liga o replay agendado. |
| `mop.client.retry.replay.initial-delay-ms` | `MOP_CLIENT_RETRY_REPLAY_INITIAL_DELAY_MS` | `15000` | (igual) | Atraso da primeira drenagem após o boot. |
| `mop.client.retry.replay.interval-ms` | `MOP_CLIENT_RETRY_REPLAY_INTERVAL_MS` | **`10000`** (10 s) | **`1800000`** (30 min) | ⚠️ **Divergência intencional** entre profiles — em `local` o ciclo é longo para evitar spam de log. Em prod, dimensione conforme SLA. |
| `mop.client.retry.replay.max-messages-per-tick` | `MOP_CLIENT_RETRY_REPLAY_MAX_PER_TICK` | `25` | (igual) | Lote por ciclo. |
| `mop.server.availability.enabled` | `MOP_SERVER_AVAILABILITY_CHECK_ENABLED` | `true` | (igual) | Sondas HTTP de disponibilidade do MOP. |
| `mop.server.availability.check-interval-ms` | `MOP_SERVER_AVAILABILITY_CHECK_INTERVAL_MS` | `30000` | (igual) | Intervalo entre sondas. |
| `mop.server.availability.connect-timeout-ms` | `MOP_SERVER_AVAILABILITY_CONNECT_TIMEOUT_MS` | `3000` | (igual) | Timeout de conexão. |
| `mop.server.availability.read-timeout-ms` | `MOP_SERVER_AVAILABILITY_READ_TIMEOUT_MS` | `5000` | (igual) | Timeout de leitura. |

### Circuit breakers (Resilience4j)

Declarados em `application.yml` (seção `resilience4j.circuitbreaker`):

- `mopAnonymizationConfig` — protege o GET de regras de campos.
- `mopProcessEndpoint` — protege o POST `/process`.

Configuração padrão de ambos: `slidingWindowSize=10`, `minimumNumberOfCalls=5`, `failureRateThreshold=50%`, `waitDurationInOpenState=30s`. Não há env vars dedicadas para cada parâmetro — para ajustar, edite o YAML do seu profile ou use `SPRING_APPLICATION_JSON`.

Estado pode ser inspecionado em `/actuator/health` (seção `circuitBreakers`) ou via `/actuator/metrics/resilience4j.circuitbreaker.state`.

---

## Cache

Caffeine local. Ver `application.yml` (`spring.cache` + `cache.*`).

| Propriedade | Variável | Padrão |
|---|---|---|
| `cache.max-size` | `CACHE_MAX_SIZE` | `10000` |
| `cache.open-api-spec.ttl-seconds` | `CACHE_OPEN_API_SPEC_TTL_SECONDS` | `3600` |
| `cache.app-config.ttl-seconds` | `CACHE_APP_CONFIG_TTL_SECONDS` | `1800` |
| `cache.normalized-endpoints.ttl-seconds` | `CACHE_NORMALIZED_ENDPOINTS_TTL_SECONDS` | `300` |

`spring.cache.caffeine.spec`: `maximumSize=1000,expireAfterWrite=5m` (cache genérico do Spring).

---

## Actuator

| Propriedade | Variável | Padrão (base) |
|---|---|---|
| `management.endpoints.web.exposure.include` | `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `*` |
| `management.endpoint.health.show-details` | `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | `always` |
| `management.metrics.tags.application` | — | `${spring.application.name}` (= `mop-client-gateway`) |

> [!WARNING]
> O default `*` expõe **todos** os endpoints (incluindo `/env`, `/configprops`, `/loggers`, `/threaddump`) — risco de vazamento de `JWS_PRIVATE_KEY` e demais secrets via `/actuator/env`. **Em produção, restrinja explicitamente:**
>
> ```bash
> export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
> export MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when_authorized
> ```

Com `context-path=/v1/anonymize`, todos os endpoints ficam sob `/v1/anonymize/actuator/*` (ex.: `/v1/anonymize/actuator/health`).

---

## Logging

| Propriedade | Variável | Padrão |
|---|---|---|
| `logging.level.root` | `LOG_LEVEL_ROOT` | `INFO` |
| `logging.level.br.com.opin.mopclient.gateway` | `LOG_LEVEL_GATEWAY` | `DEBUG` |

> Em produção considere `LOG_LEVEL_GATEWAY=INFO` para reduzir volume e configurar appender JSON (ECS/Logstash) para que `correlationId` seja indexado.

---

## Referência rápida — sandbox

```bash
export SPRING_PROFILES_ACTIVE=local

# MOP — sandbox OPIN
export EXTERNAL_REQUEST_URL=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process
export EXTERNAL_API_DATA_ANONYMIZATION=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent

# RabbitMQ local
export RABBITMQ_VALIDATOR_HOST=localhost
export RABBITMQ_VALIDATOR_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest

# Assinatura JWS (obrigatório — credenciais do participante)
export MOP_PAYLOAD_SIGNING_ENABLED=true
export JWS_PRIVATE_KEY="$(cat ./mop-client-sandbox.pem)"
export JWS_KID=<seu-kid-publicado-no-JWKS>
export JWS_ORG_ID=<seu-orgId-uuid>

# Retry (opcional — usa defaults sãos)
export MOP_CLIENT_RETRY_QUEUE=mop.client.retry.queue
```

Windows PowerShell: substitua `export VAR=valor` por `$env:VAR = "valor"`.
