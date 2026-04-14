# Variáveis de ambiente e propriedades

Este documento descreve as principais variáveis de ambiente e propriedades Spring Boot do **MOP Client Gateway**, conforme `src/main/resources/application.yml` e `src/main/resources/application-local.yml`.

**Convenção:** o Spring Boot aceita nomes em **MAIÚSCULAS** com `_` no lugar de `.` (relaxed binding).

---

## Perfil Spring

| Propriedade | Variável de ambiente | Padrão (base) | Descrição |
|-------------|----------------------|---------------|-----------|
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `default` em `application.yml` | Perfis ativos (ex.: `local`). |

---

## Servidor HTTP

| Propriedade | Variável de ambiente | Onde | Padrão | Descrição |
|-------------|----------------------|------|--------|-----------|
| `server.port` | `SERVER_PORT` | `application-local.yml` | `8080` | Porta HTTP. |
| `server.servlet.context-path` | `SERVER_CONTEXT_PATH` | `application-local.yml` | `/v1/anonymize` | Prefixo dos endpoints (ex.: `POST …/v1/anonymize/data`). |

No `application.yml` base, `server.port` é `8080` e `context-path` é `/v1/anonymize` (sem variáveis).

---

## APIs externas (MOP)

### POST — processamento downstream

| Propriedade | Variável | Descrição |
|-------------|----------|-------------|
| `external.server.request.url` | — (fixo no YAML base) | URL usada pelo cliente HTTP de envio ao MOP (`ExternalApiClient`). |
| `external.server.request.url` | `EXTERNAL_REQUEST_URL` | Em **`application-local.yml`**, sobrescreve a URL do POST. |
| `external.request.url` | `EXTERNAL_REQUEST_URL` | Modelo alternativo no YAML base; alinhar com a URL efetiva do POST na sua implantação. |
| `external.request.host` | `EXTERNAL_REQUEST_HOST` | Host (complementar). |
| `external.request.path` | `EXTERNAL_REQUEST_PATH` | Caminho (padrão `/process`). |
| `external.request.method` | `EXTERNAL_REQUEST_METHOD` | Método (padrão `POST`). |

### GET — regras de campos (configuração dinâmica)

| Propriedade | Variável | Padrão no `application.yml` |
|-------------|----------|------------------------------|
| `external.api.data-anonymization` | `EXTERNAL_API_DATA_ANONYMIZATION` | `http://mop-server-entrypoint-dev.intranet.opinbrasil/anonymization-fields?schema=Consent` |

---

## RabbitMQ

O projeto inclui **`spring-boot-starter-amqp`**. O broker é usado para a **fila de retry do cliente** (`mop.client.retry.queue`), não para o modelo antigo de “pipeline só por filas”.

| Propriedade | Variável de ambiente | Padrão |
|-------------|----------------------|--------|
| `spring.rabbitmq.host` | `RABBITMQ_VALIDATOR_HOST` | `localhost` |
| `spring.rabbitmq.port` | `RABBITMQ_VALIDATOR_PORT` | `5672` |
| `spring.rabbitmq.username` | `RABBITMQ_USERNAME` | `guest` |
| `spring.rabbitmq.password` | `RABBITMQ_PASSWORD` | `guest` |

No profile `local`, existem ainda propriedades de *listener* e nomes de filas (`RABBITMQ_VALIDATOR_QUEUE_NAME`, `RABBITMQ_OUTPUT_QUEUE_NAME`, `RABBITMQ_CONCURRENCY`, etc.) herdadas de configuração antiga — só são relevantes se algum componente ainda as consumir; a fila ativa de retry é definida em `mop.client.retry.queue`.

---

## Retry do cliente e disponibilidade do MOP

Prefixo: `mop.client.retry` e `mop.server.availability` (ver `application.yml`).

| Propriedade | Variável de ambiente | Padrão | Descrição |
|-------------|----------------------|--------|-----------|
| `mop.client.retry.queue` | `MOP_CLIENT_RETRY_QUEUE` | `mop.client.retry.queue` | Nome da fila AMQP para mensagens adiadas. |
| `mop.client.retry.replay.enabled` | `MOP_CLIENT_RETRY_REPLAY_ENABLED` | `true` | Liga o *replay* agendado. |
| `mop.client.retry.replay.interval-ms` | `MOP_CLIENT_RETRY_REPLAY_INTERVAL_MS` | `10000` | Intervalo entre tentativas de drenar a fila. |
| `mop.client.retry.replay.max-messages-per-tick` | `MOP_CLIENT_RETRY_REPLAY_MAX_PER_TICK` | `25` | Limite de mensagens por ciclo. |
| `mop.server.availability.enabled` | `MOP_SERVER_AVAILABILITY_CHECK_ENABLED` | `true` | Sondas HTTP de disponibilidade do MOP. |
| `mop.server.availability.check-interval-ms` | `MOP_SERVER_AVAILABILITY_CHECK_INTERVAL_MS` | `30000` | Intervalo entre sondas. |
| `mop.server.availability.connect-timeout-ms` | `MOP_SERVER_AVAILABILITY_CONNECT_TIMEOUT_MS` | `3000` | Timeout de conexão da sonda. |
| `mop.server.availability.read-timeout-ms` | `MOP_SERVER_AVAILABILITY_READ_TIMEOUT_MS` | `5000` | Timeout de leitura da sonda. |

Os *circuit breakers* Resilience4j (`mopAnonymizationConfig`, `mopProcessEndpoint`) estão declarados em `application.yml`; ajustes finos costumam ser feitos no YAML (não há variáveis de ambiente dedicadas para cada parâmetro do circuito, salvo extensão via `SPRING_APPLICATION_JSON`).

---

## Cache

| Propriedade | Variável de ambiente | Padrão |
|-------------|----------------------|--------|
| `cache.max-size` | `CACHE_MAX_SIZE` | `10000` |
| `cache.open-api-spec.ttl-seconds` | `CACHE_OPEN_API_SPEC_TTL_SECONDS` | `3600` |
| `cache.app-config.ttl-seconds` | `CACHE_APP_CONFIG_TTL_SECONDS` | `1800` |
| `cache.normalized-endpoints.ttl-seconds` | `CACHE_NORMALIZED_ENDPOINTS_TTL_SECONDS` | `300` |

Há também `spring.cache` com Caffeine em `application.yml`.

---

## Actuator

| Propriedade | Variável | Padrão base |
|-------------|----------|-------------|
| `management.endpoints.web.exposure.include` | `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `*` |
| `management.endpoint.health.show-details` | `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | `always` |

Com `server.servlet.context-path=/v1/anonymize`, o health fica em **`/v1/anonymize/actuator/health`**.

No profile `local`, `management.endpoints.web.exposure.include` pode ser definido via **`MANAGEMENT_ENDPOINTS_INCLUDE`** (placeholder em `application-local.yml`).

---

## Logging

| Propriedade | Variável de ambiente | Padrão |
|-------------|----------------------|--------|
| `logging.level.root` | `LOG_LEVEL_ROOT` | `INFO` |
| `logging.level.br.com.opin.mopclient.gateway` | `LOG_LEVEL_GATEWAY` | `DEBUG` |

---

## Referência rápida

```bash
export SPRING_PROFILES_ACTIVE=local
export RABBITMQ_VALIDATOR_HOST=localhost
export EXTERNAL_API_DATA_ANONYMIZATION=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent
export EXTERNAL_REQUEST_URL=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process
export MOP_CLIENT_RETRY_QUEUE=mop.client.retry.queue
```

(Windows: `set VAR=valor`.)
