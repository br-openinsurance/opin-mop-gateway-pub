# Variáveis de ambiente e propriedades

Referência completa das variáveis e propriedades Spring Boot do **MOP Client**, conforme `src/main/resources/application.yml` e `src/main/resources/application-local.yml`.

> **Convenção:** o Spring Boot aceita nomes em **MAIÚSCULAS** com `_` no lugar de `.` (relaxed binding). Ex.: `mop.endpoints.process.url` ⇔ `MOP_ENDPOINTS_PROCESS_URL` (também aceitamos o alias mais curto `MOP_PROCESS_URL` como placeholder no YAML).

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
10. [Referência rápida — endpoints MOP (sandbox e produção)](#referência-rápida--endpoints-mop-sandbox-e-produção)

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

Todos os endpoints do MOP usados pelo gateway ficam sob o prefixo `mop.endpoints.*`. Cada endpoint exige a **URL completa**.

### POST — envio do payload processado (`/process`)

| Propriedade | Variável | Padrão (base) | Descrição |
|---|---|---|---|
| `mop.endpoints.process.url` | `MOP_PROCESS_URL` | **sem default** ⚠️ | URL completa do `POST /process` no MOP. **Obrigatória** — a aplicação falha no boot se ausente. |
| `mop.endpoints.process.method` | `MOP_PROCESS_METHOD` | `POST` | Método HTTP. |

### GET — regras de campos (configuração dinâmica de anonimização)

| Propriedade | Variável | Padrão | Descrição |
|---|---|---|---|
| `mop.endpoints.anonymization-config.url` | `MOP_ANONYMIZATION_CONFIG_URL` | **sem default** ⚠️ | URL completa do `GET` de regras de campos. **Obrigatória** — defina explicitamente em todo ambiente. Ver [sandbox e produção](#referência-rápida--endpoints-mop-sandbox-e-produção). |

---

## Assinatura JWS

Configura a assinatura do payload final enviado ao MOP. **Obrigatória em produção**, opcional em desenvolvimento local.

| Propriedade | Variável | Padrão | Descrição |
|---|---|---|---|
| `mop.payload-signing.enabled` | `MOP_PAYLOAD_SIGNING_ENABLED` | `true` | Liga/desliga a assinatura JWS. Quando `true`, exige `JWS_PRIVATE_KEY`, `JWS_KID` e `JWS_ORG_ID` (validados no startup). Quando `false`, o payload é enviado **sem assinatura** (ver regras abaixo). |
| `mop.payload-signing.private-key-pem` | `JWS_PRIVATE_KEY` | **sem default** ⚠️ | Chave privada PKCS#8 em PEM. No `application.yml` base, é referenciada sem default (`${JWS_PRIVATE_KEY}`), então deve estar definida quando o profile base é carregado e, em especial, quando `MOP_PAYLOAD_SIGNING_ENABLED=true`. |
| `mop.payload-signing.key-id` | `JWS_KID` | **sem default** ⚠️ | `kid` no header JWT — deve casar com chave publicada no JWKS do participante. No `application.yml` base, é referenciado sem default (`${JWS_KID}`), então deve estar definido quando `MOP_PAYLOAD_SIGNING_ENABLED=true`. |
| `mop.payload-signing.org-id` | `JWS_ORG_ID` | **sem default** ⚠️ | `orgId` (UUID) nas claims do JWT e em `trace.OrgId`. No `application.yml` base, é referenciado sem default (`${JWS_ORG_ID}`) e é validado no startup. |

**Algoritmo:** `PS256` (RSA-PSS / SHA-256).

### Modos efetivos

| `JWS_PRIVATE_KEY` | `MOP_PAYLOAD_SIGNING_ENABLED` | Comportamento |
|---|---|---|
| definido | `true` (default) | ✅ **Assinado** (recomendado) |
| definido | `false` | ⚠️ **Sem assinatura** — payload sai **unsigned** mesmo com chave configurada (útil para ambientes/dev específicos; não recomendado em produção). |
| vazio | `true` (default) | ❌ **Erro no boot** (`enabled=true` exige chave + `kid` + `orgId`) |
| vazio | `false` | ❌ **Erro no boot** (no base YAML, as variáveis `JWS_*` são placeholders sem default) |

> [!CAUTION]
> Em produção, **não** passe `JWS_PRIVATE_KEY` como string nua de env var. Monte como secret via Kubernetes Secret, Vault ou AWS Secrets Manager e leia para a env apenas no entrypoint do container.

> [!WARNING]
> O modo **unsigned passthrough** é uma rede de segurança para desenvolvimento. Em produção, configure as três variáveis (`JWS_PRIVATE_KEY`, `JWS_KID`, `JWS_ORG_ID`) e monitore o log de boot — a presença do WARN `[JWS] No private key configured` é um indicador de configuração faltando.

---

## RabbitMQ

O projeto inclui `spring-boot-starter-amqp`. O broker é usado para as filas de **retry** e **DLQ** do cliente. **RabbitMQ é obrigatório**: sem ele, a aplicação não sobe.

### Criação obrigatória das filas

Antes de subir o gateway (ou ao provisionar o broker em sandbox/produção), **crie as duas filas** no RabbitMQ com os nomes configurados em `mop.client.retry.queue` e `mop.client.retry.dlq.queue`:

| Fila | Variável | Nome padrão | Função |
|------|----------|-------------|--------|
| **Retry** | `MOP_CLIENT_RETRY_QUEUE` | `mop.client.retry.queue` | Enfileira payloads quando o MOP está indisponível; drenada pelo replay agendado. |
| **DLQ** | `MOP_CLIENT_RETRY_DLQ_QUEUE` | `mop.client.retry.dlq` | Recebe mensagens após esgotar tentativas ou quando o payload é inválido. |

**Requisitos:** filas **duráveis** (`durable=true`). Se usar nomes customizados, defina `MOP_CLIENT_RETRY_QUEUE` e `MOP_CLIENT_RETRY_DLQ_QUEUE` **antes** da criação, com o **mesmo** valor em todos os pods/instâncias.

Exemplo (CLI `rabbitmqadmin`, com broker acessível):

```bash
rabbitmqadmin declare queue name=mop.client.retry.queue durable=true
rabbitmqadmin declare queue name=mop.client.retry.dlq durable=true
```

Via **Management UI** (ex.: http://localhost:15672): **Queues** → **Add a new queue** → informe o nome → marque **Durable** → repita para a segunda fila.

> Em ambiente local com `mvn spring-boot:run`, o Spring AMQP pode **declarar** as filas no boot se o usuário do broker tiver permissão. Em **sandbox/produção**, trate a criação como **pré-requisito de infraestrutura** — não dependa apenas da auto-declaração na subida da aplicação.

| Propriedade | Variável | Padrão |
|---|---|---|
| `spring.rabbitmq.host` | `RABBITMQ_VALIDATOR_HOST` | **sem default** ⚠️ |
| `spring.rabbitmq.port` | `RABBITMQ_VALIDATOR_PORT` | **sem default** ⚠️ |
| `spring.rabbitmq.username` | `RABBITMQ_USERNAME` | **sem default** ⚠️ |
| `spring.rabbitmq.password` | `RABBITMQ_PASSWORD` | **sem default** ⚠️ |

> Todas são **obrigatórias**: a aplicação falha no boot com `Could not resolve placeholder` se qualquer uma estiver ausente. Para dev local típico, exporte `RABBITMQ_VALIDATOR_HOST=localhost`, `_PORT=5672`, `RABBITMQ_USERNAME=guest`, `RABBITMQ_PASSWORD=guest`.

> O sufixo `_VALIDATOR_` é herdado do antigo serviço descontinuado (validator). Será renomeado para `RABBITMQ_HOST` / `RABBITMQ_PORT` em uma próxima versão, com alias mantido.

> No profile `local` ainda existem propriedades antigas de listener e nomes de filas (`RABBITMQ_VALIDATOR_QUEUE_NAME`, `RABBITMQ_OUTPUT_QUEUE_NAME`, `RABBITMQ_CONCURRENCY`). **Estão obsoletas para o fluxo atual**. As filas ativas são **`mop.client.retry.queue`** e **`mop.client.retry.dlq`** — ambas devem existir no broker (ver [Criação obrigatória das filas](#criação-obrigatória-das-filas)).

---

## Retry do cliente e disponibilidade do MOP

Prefixo: `mop.client.retry` e `mop.server.availability`.

| Propriedade | Variável | Padrão (base) | Padrão (local) | Descrição |
|---|---|---|---|---|
| `mop.rabbit.require-at-startup` | `MOP_RABBIT_REQUIRE_AT_STARTUP` | `true` (fixo no base) | `true` (configurável) | No `application.yml` base o valor está fixo em `true`; no profile `local` pode ser configurado por env var. Se `true`, o arranque falha se o broker AMQP não estiver acessível (`ApplicationRunner`). |
| `mop.client.retry.queue` | `MOP_CLIENT_RETRY_QUEUE` | `mop.client.retry.queue` | (igual) | Nome da fila AMQP. |
| `mop.client.retry.replay.enabled` | `MOP_CLIENT_RETRY_REPLAY_ENABLED` | `true` | (igual) | Liga o replay agendado. |
| `mop.client.retry.replay.initial-delay-ms` | `MOP_CLIENT_RETRY_REPLAY_INITIAL_DELAY_MS` | `60000` (1 min) | (igual) | Atraso da primeira drenagem após o boot. |
| `mop.client.retry.replay.interval-ms` | `MOP_CLIENT_RETRY_REPLAY_INTERVAL_MS` | `60000` (1 min) | (igual) | Intervalo entre ciclos de dreno (`fixedDelay`). |
| `mop.client.retry.replay.max-messages-per-tick` | `MOP_CLIENT_RETRY_REPLAY_MAX_PER_TICK` | `25` | (igual) | Lote por ciclo. |
| `mop.client.retry.dlq.queue` | `MOP_CLIENT_RETRY_DLQ_QUEUE` | `mop.client.retry.dlq` | (igual) | Fila Dead-Letter para eventos que excederam tentativas ou são inválidos. |
| `mop.client.retry.dlq.max-attempts` | `MOP_CLIENT_RETRY_DLQ_MAX_ATTEMPTS` | `5` | (igual) | Máximo de falhas de replay antes de mover para a DLQ. |
| `mop.server.availability.enabled` | `MOP_SERVER_AVAILABILITY_CHECK_ENABLED` | `true` | (igual) | Sondas HTTP de disponibilidade do MOP. |
| `mop.server.availability.check-interval-ms` | `MOP_SERVER_AVAILABILITY_CHECK_INTERVAL_MS` | `30000` | (igual) | Intervalo entre sondas. |
| `mop.server.availability.connect-timeout-ms` | `MOP_SERVER_AVAILABILITY_CONNECT_TIMEOUT_MS` | `3000` | (igual) | Timeout de conexão. |
| `mop.server.availability.read-timeout-ms` | `MOP_SERVER_AVAILABILITY_READ_TIMEOUT_MS` | `5000` | (igual) | Timeout de leitura. |

### Disjuntores de circuito (Resilience4j)

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

## Logs

| Propriedade | Variável | Padrão |
|---|---|---|
| `logging.level.root` | `LOG_LEVEL_ROOT` | `INFO` |
| `logging.level.br.com.opin.mopclient.gateway` | `LOG_LEVEL_GATEWAY` | `DEBUG` |

> Em produção considere `LOG_LEVEL_GATEWAY=INFO` para reduzir volume e configurar appender JSON (ECS/Logstash) para que `correlationId` seja indexado.

---

## Referência rápida — endpoints MOP (sandbox e produção)

**Host base produção (oficial):** `https://mop-server-entrypoint.opinbrasil.com.br/`  
**Host base sandbox:** `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/`

| Ambiente | Variável | URL |
|----------|----------|-----|
| **Sandbox** | `EXTERNAL_REQUEST_URL` / `MOP_PROCESS_URL` | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process` |
| **Sandbox** | `EXTERNAL_API_DATA_ANONYMIZATION` / `MOP_ANONYMIZATION_CONFIG_URL` | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent` |
| **Produção** | `EXTERNAL_REQUEST_URL` / `MOP_PROCESS_URL` | `https://mop-server-entrypoint.opinbrasil.com.br/process` |
| **Produção** | `EXTERNAL_API_DATA_ANONYMIZATION` / `MOP_ANONYMIZATION_CONFIG_URL` | `https://mop-server-entrypoint.opinbrasil.com.br/anonymization-fields?schema=Consent` |

> Em produção, use credenciais JWS e JWKS do participante **cadastrados no ambiente de produção** — não reutilize `kid`/chave do sandbox.

### Exemplo — sandbox (desenvolvimento / homologação contra sandbox OPIN)

```bash
export SPRING_PROFILES_ACTIVE=local

# MOP — sandbox OPIN
export EXTERNAL_REQUEST_URL=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process
export EXTERNAL_API_DATA_ANONYMIZATION=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent
# aliases aceitos pelo Spring (relaxed binding):
# export MOP_PROCESS_URL=...
# export MOP_ANONYMIZATION_CONFIG_URL=...

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

### Exemplo — produção

```bash
# MOP — produção OPIN
export EXTERNAL_REQUEST_URL=https://mop-server-entrypoint.opinbrasil.com.br/process
export EXTERNAL_API_DATA_ANONYMIZATION=https://mop-server-entrypoint.opinbrasil.com.br/anonymization-fields?schema=Consent

export MOP_PAYLOAD_SIGNING_ENABLED=true
# JWS_PRIVATE_KEY, JWS_KID, JWS_ORG_ID — via Secret (produção)
# RABBITMQ_* — broker institucional
```

Windows PowerShell: substitua `export VAR=valor` por `$env:VAR = "valor"`.
