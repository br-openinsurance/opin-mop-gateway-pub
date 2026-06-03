# MOP Client

API HTTP **auto-hospedada** que cada participante do **Open Insurance Brasil** instala em seu ambiente para enviar eventos de trace ao **MOP Server**. Em uma única aplicação, executa: validação → anonimização → assinatura JWS → POST ao MOP, com **circuit breaker** e **fila de retry** quando o MOP está indisponível.


> [!IMPORTANT]
> Este repositório **substitui** os antigos `mop-client-data-validator-pub` e `opin-mop-client-anonymization-pub`. Não é necessário implantar/configurar aqueles componentes — basta atualizar a imagem deste gateway.

> [!NOTE]
> **Endpoints do MOP Server (por ambiente)**
>
> Configure `EXTERNAL_REQUEST_URL` e `EXTERNAL_API_DATA_ANONYMIZATION` com a URL **completa** do ambiente em que o gateway irá operar. Credenciais JWS (`JWS_KID`, `JWS_ORG_ID`, chave privada) devem estar cadastradas no **mesmo** ambiente (JWKS).
>
> | Ambiente | Host base (MOP Server) | `POST /process` (`EXTERNAL_REQUEST_URL`) | `GET` regras (`EXTERNAL_API_DATA_ANONYMIZATION`) |
> |----------|------------------------|------------------------------------------|--------------------------------------------------|
> | **Sandbox** | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/` | `.../process` | `.../anonymization-fields?schema=Consent` |
> | **Produção** | **`https://mop-server-entrypoint.opinbrasil.com.br/`** | `https://mop-server-entrypoint.opinbrasil.com.br/process` | `https://mop-server-entrypoint.opinbrasil.com.br/anonymization-fields?schema=Consent` |
>
> - **GET `.../anonymization-fields?schema=Consent`**: endpoint de **configuração**. Retorna as regras dinâmicas de campos (**quais devem ser anonimizados** e **quais podem ficar expostos**) para o schema informado (ex.: `Consent`). O gateway chama esse endpoint no **início do processamento** (antes de anonimizar) e também pode usá-lo como **sonda de disponibilidade** do MOP.
> - **POST `.../process`**: endpoint de **processamento/ingestão**. Recebe o **payload final** que o gateway montou e anonimizado. No fluxo padrão, o corpo enviado ao MOP é um **JWT compacto** (`Content-Type: application/jwt`) quando a assinatura está habilitada.
>
> Em resumo: **GET = “quais campos anonimizar”**; **POST = “enviar o evento já anonimizado (e assinado)”**. Detalhes de variáveis: [`docs/VARIAVEIS_DE_AMBIENTE.md`](docs/VARIAVEIS_DE_AMBIENTE.md).

---

## Sumário

1. [Como funciona em 30 segundos](#como-funciona-em-30-segundos)
2. [Início rápido — rodando em até 10 minutos](#início-rápido--rodando-em-at\u00e9-10-minutos)
3. [Instalação em Kubernetes (Helm)](#instalação-em-kubernetes-helm)
4. ⚠️ [Antes de ir para produção](#antes-de-ir-para-produção) — **leitura obrigatória**
5. [Contrato da API](#contrato-da-api)
6. [Configuração](#configuração)
7. [Segurança e assinatura JWS](#segurança-e-assinatura-jws)
8. [Observabilidade](#observabilidade)
9. [Solução de problemas](#solução-de-problemas)
10. [Referências](#referências)


---

## Como funciona em 30 segundos

1. **Sua aplicação** envia `POST /v1/anonymize/data` com os headers de trace.
2. **O gateway** valida os headers, anonimiza o payload e assina.
3. **Envia ao MOP** — se o MOP estiver fora, **enfileira no RabbitMQ** e tenta de novo automaticamente.
4. **Você recebe `HTTP 200 SUCCESS`** *nos dois casos*. Quando o MOP volta, o replay drena a fila.

```mermaid
flowchart LR
    A([Sua aplicação]) --> G[MOP Client]
    G -->|MOP no ar| M[(MOP Server)]
    G -.->|MOP fora| Q[(Fila de retry)]
    Q -.->|replay| M
```

> [!CAUTION]
> A resposta `HTTP 200 SUCCESS` cobre **os dois cenários** (entregue ou enfileirado) com o **mesmo body**. Para saber qual aconteceu, olhe os **logs** e a **profundidade da fila**. Trate como at-least-once e leia [Antes de ir para produção](#antes-de-ir-para-produção).

---

## Instalação em Kubernetes (Helm)

Em produção, o **MOP Client** pode ser implantado com o **Helm Chart** publicado no GHCR (`ghcr.io/br-openinsurance/mop-client-chart/mop-client`). O guia oficial (pré-requisitos, `values-client.yaml`, install/upgrade e verificação) está no repositório de publicação:

- **[Instalação via Helm — `INSTALA_MOP_CLIENT.md`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/feat/mop-client-install/docs/INSTALA_MOP_CLIENT.md)** (branch `feat/mop-client-install`)
- Resumo e links neste repositório: [`docs/INSTALACAO.md`](docs/INSTALACAO.md)

---

## Início rápido — rodando em até 10 minutos

> Este caminho leva você de "nunca vi o projeto" até um `200 OK` válido contra o **sandbox OPIN**. Pré-requisito: você já obteve as credenciais JWS do participante (chave privada PEM + `kid` publicado no JWKS + `orgId`). Se ainda não tem, obtenha-as antes — o gateway **não sobe sem elas**.

### Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| Java | **17+** | `java -version` |
| Maven | 3.x | `mvn -v` |
| Docker / Docker Compose | qualquer | Para subir o RabbitMQ |
| Credenciais JWS | — | `mop-client-sandbox.pem` (PKCS#8), `JWS_KID`, `JWS_ORG_ID` |

### Passo 1 — Clonar e subir (~2 min)

```bash
git clone --branch develop https://github.com/br-openinsurance/opin-mop-gateway-pub.git
cd opin-mop-gateway-pub
docker compose up -d
```

Confira que o RabbitMQ está saudável:

```bash
docker compose ps
# rabbitmq   running   0.0.0.0:5672->5672, 0.0.0.0:15672->15672
```

UI de gestão (opcional): http://localhost:15672 (`guest`/`guest`).

### Passo 2 — Configurar variáveis (~3 min)

Crie um arquivo `.env.sandbox` (ou exporte no shell). **Todas as 8 variáveis abaixo são obrigatórias** — sem elas a aplicação não sobe.

<details>
<summary><b>Linux / macOS (bash/zsh)</b></summary>

```bash
export SPRING_PROFILES_ACTIVE=local

# Endpoints do MOP (sandbox OPIN)
export EXTERNAL_REQUEST_URL="https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process"
export EXTERNAL_API_DATA_ANONYMIZATION="https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent"

# RabbitMQ local (do docker compose)
export RABBITMQ_VALIDATOR_HOST=localhost
export RABBITMQ_VALIDATOR_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest

# Assinatura JWS (obrigatório — credenciais do participante)
export MOP_PAYLOAD_SIGNING_ENABLED=true
export JWS_PRIVATE_KEY="$(cat ./mop-client-sandbox.pem)"
export JWS_KID="<seu-kid-publicado-no-JWKS>"
export JWS_ORG_ID="<seu-orgId-uuid>"
```

</details>

<details>
<summary><b>Windows PowerShell</b></summary>

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"

$env:EXTERNAL_REQUEST_URL = "https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process"
$env:EXTERNAL_API_DATA_ANONYMIZATION = "https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent"

$env:RABBITMQ_VALIDATOR_HOST = "localhost"
$env:RABBITMQ_VALIDATOR_PORT = "5672"
$env:RABBITMQ_USERNAME = "guest"
$env:RABBITMQ_PASSWORD = "guest"

$env:MOP_PAYLOAD_SIGNING_ENABLED = "true"
$env:JWS_PRIVATE_KEY = (Get-Content -Raw .\mop-client-sandbox.pem)
$env:JWS_KID = "<seu-kid-publicado-no-JWKS>"
$env:JWS_ORG_ID = "<seu-orgId-uuid>"
```

</details>

> [!WARNING]
> No `application.yml` base, `MOP_PAYLOAD_SIGNING_ENABLED` tem default `true`.

### Passo 3 — Subir a aplicação (~2 min)

```bash
mvn spring-boot:run
```

Quando vir `Started MopClientApplication in X.X seconds (process running for ...)` a aplicação está pronta na porta **8080** com context-path `/v1/anonymize`.

### Passo 4 — Smoke test (~1 min)

Health check:

```bash
curl -s http://localhost:8080/v1/anonymize/actuator/health | jq .
# Espere: {"status":"UP", ...} com circuitBreakers e rabbit em UP
```

Primeira requisição:

```bash
curl -i -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: $(uuidgen)" \
  -H "origin: client" \
  -H "path: /open-insurance/consents/v2/consents" \
  -H "operation: POST" \
  -H "step: consent-created" \
  -H "dataEventoStep: 2026-04-27T11:00:00Z" \
  -H "clientSSId: RECEPTORA-A" \
  -H "serverASId: TRANSMISSORA-B" \
  -H "Content-Type: application/json" \
  -d '{"data":{"id":"123"}}'
```

Resposta esperada:

```http
HTTP/1.1 200
Content-Type: application/json

{
  "status": "SUCCESS",
  "message": "Request processed successfully. Your data has been received and forwarded to the server.",
  "correlationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "timestamp": "2026-04-27T11:00:01.234Z",
  "clientSSId": "RECEPTORA-A",
  "serverASId": "TRANSMISSORA-B",
  "path": "/open-insurance/consents/v2/consents",
  "operation": "POST"
}
```

### Passo 5 — Validar entrega real ao MOP (~2 min)

Como o body de `200` é o **mesmo** quando a mensagem foi entregue e quando ela apenas foi enfileirada, **sempre** valide pelos logs:

```bash
# Log de entrega confirmada:
# "Payload successfully processed. clientSSId=... correlationId=..."

# Log de enfileiramento (MOP indisponível):
# "[MOP retry] Client received HTTP 200; body sent to retry queue | correlationId=... | mopReportId=... | dateTime=..."
```

E pela profundidade da fila no RabbitMQ:

```bash
# Via Management UI (http://localhost:15672) → Queues → mop.client.retry.queue
# Mensagens em "Ready" devem cair para zero em até MOP_CLIENT_RETRY_REPLAY_INTERVAL_MS
```

✅ Se o log mostrou `Payload successfully processed`, você está integrado.

---

## Antes de ir para produção

Esta seção lista os **riscos operacionais reais** do gateway. Não pule.

### 1. ⚠️ HTTP 200 não garante entrega ao MOP

O gateway retorna `200 status=SUCCESS` em **dois cenários distintos** com o **mesmo body**:

| Cenário | Status HTTP | Campo `status` | Campo `message` | Como detectar |
|---|---|---|---|---|
| Entregue ao MOP | 200 | `SUCCESS` | `"Request processed successfully..."` | Log: `Payload successfully processed` |
| **Apenas enfileirado para retry** | 200 | `SUCCESS` | `"Request processed successfully..."` *(mesmo texto)* | Log: `[MOP retry] Client received HTTP 200; body sent to retry queue` |

**Implicações para produção:**

- Não confie apenas no status HTTP para confirmar entrega.
- Monitore: profundidade da fila `mop.client.retry.queue`, taxa de eventos `[MOP retry]`, estado dos circuit breakers `mopProcessEndpoint` e `mopAnonymizationConfig` no `actuator/health`.
- Modelo de entrega = **at-least-once**. O servidor MOP precisa deduplicar por `X-Correlation-Id` ou `mopReportId`. **Use UUID por intenção lógica** e nunca repita um `correlationId` para operações distintas.
- Se o MOP ficar fora por mais que `(tamanho_máx_da_fila × tempo_médio_da_mensagem)`, mensagens novas começam a falhar. Dimensione o broker para o pior cenário.

### 2. RabbitMQ é obrigatório no boot e durante operação

Sem RabbitMQ a aplicação **não sobe**. Em runtime, se o broker cair:
- Mensagens novas que precisariam ir para retry **falham**.
- Mensagens já enfileiradas ficam intactas (persistentes), mas não serão drenadas.

**Recomendações:** RabbitMQ em cluster com `quorum queue` (não classic), TTL configurado para a fila de retry, e DLQ separada (`mop.client.retry.dlq`).

### 3. Chave privada JWS em variável de ambiente é desencorajada

`JWS_PRIVATE_KEY` recebe o PEM completo. Em produção:
- Monte o PEM como **arquivo via secret** (Kubernetes Secret, AWS Secrets Manager, Vault) e leia para a env var apenas no entrypoint do container.
- Garanta rotação documentada — coordenando com a publicação do JWKS para evitar janela de `401`.
- Habilite `MOP_PAYLOAD_SIGNING_ENABLED=true` **explicitamente** (produção) e garanta `JWS_PRIVATE_KEY`/`JWS_KID`/`JWS_ORG_ID` preenchidos.

### 4. `kid` precisa estar publicado no JWKS antes do primeiro request

O servidor MOP responde **`401`** se o `kid` do JWT não casar com nenhuma chave do JWKS publicado pelo participante. Sequência segura:

1. Gerar par de chaves.
2. Publicar a pública no JWKS do participante (com o `kid` correspondente).
3. Aguardar o cache do MOP refletir (~minutos).
4. **Só então** subir o gateway com a nova chave.

### 5. Headers HTTP fora de convenção

Os headers `origin`, `path`, `operation`, `step`, `dataEventoStep`, `clientSSId`, `serverASId` **não usam o prefixo `X-`** e nem kebab-case. Riscos:

- `origin` colide com o header padrão CORS — proxies/CDN podem **sobrescrevê-lo**. Audite o caminho da requisição (Cloudflare, AWS ALB, NGINX) e garanta que esses headers passem intactos.
- Logs centralizados (Datadog, ELK) tratam `path`/`operation` como termos genéricos — adicione tags próprias para não conflitar.

### 6. Resposta de erro `400` tem **dois formatos**

| Origem do erro | Formato |
|---|---|
| Validação de header pelo controller | JSON estruturado (`status: ERROR`, `error`, `details`, `timestamp`) |
| Header obrigatório ausente / JSON malformado | **Array de strings** (`["Missing required header: ...", "Details: ..."]`) |

Seu cliente HTTP **precisa** lidar com ambos os formatos.

### 7. Checklist mínimo de produção

- [ ] `EXTERNAL_REQUEST_URL` aponta para **produção**: `https://mop-server-entrypoint.opinbrasil.com.br/process` (não usar host sandbox).
- [ ] `EXTERNAL_API_DATA_ANONYMIZATION` aponta para **produção**: `https://mop-server-entrypoint.opinbrasil.com.br/anonymization-fields?schema=Consent`.
- [ ] `MOP_PAYLOAD_SIGNING_ENABLED=true` **e** `JWS_PRIVATE_KEY`/`JWS_KID`/`JWS_ORG_ID` definidos.
- [ ] `kid` publicado no JWKS e propagado.
- [ ] RabbitMQ com persistência, em cluster, monitorado (profundidade de fila, conexões).
- [ ] Logs em JSON estruturado, exportados com `correlationId` indexado.
- [ ] Alertas em: `mop.client.retry.queue.depth > X`, circuit `mopProcessEndpoint == OPEN > Y minutos`, taxa de `[MOP retry]` por minuto.
- [ ] Health check `/v1/anonymize/actuator/health` integrado ao orquestrador (Kubernetes liveness/readiness).
- [ ] Variáveis sensíveis fora de logs e fora do dump da JVM.

---

## Contrato da API

### Endpoint

**`POST /v1/anonymize/data`** — `Content-Type: application/json` · body opcional (corpo vazio ou inválido é normalizado para `{}`).

### Headers obrigatórios

| Header | Descrição | Restrições | Exemplo |
|---|---|---|---|
| `X-Correlation-Id` | ID da intenção lógica (idempotência at-least-once). | Não vazio, ≥ 1 char. **Recomendado: UUID v4.** | `f47ac10b-58cc-4372-a567-0e02b2c3d479` |
| `origin` | Origem da chamada. | Exatamente `client` ou `server` (case-insensitive). | `client` |
| `path` | Rota lógica do recurso OPIN. | Não vazio. | `/open-insurance/consents/v2/consents` |
| `operation` | Verbo HTTP da operação original. | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` (validado por `HttpMethod`). | `POST` |
| `clientSSId` | Identificador da receptora (SS). | Não vazio. | `RECEPTORA-A` |
| `serverASId` | Identificador da transmissora (AS). | Não vazio. | `TRANSMISSORA-B` |

**Headers opcionais:** `step` (etapa do trace; se ausente, o gateway deriva valor interno no `MessageDTO`) · `dataEventoStep` (instante ISO-8601 do passo; se ausente, usa instante atual no trace) · `traceOrigin` (origem do evento de trace; se informado, enviado ao MOP em `trace.traceOrigin` do `MessageDTO`) · `X-Mop-Reportid` (gerado se ausente).

### Resposta — `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "Request processed successfully. Your data has been received and forwarded to the server.",
  "correlationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "timestamp": "2026-04-27T11:00:01.234Z",
  "clientSSId": "RECEPTORA-A",
  "serverASId": "TRANSMISSORA-B",
  "path": "/open-insurance/consents/v2/consents",
  "operation": "POST"
}
```

> Cobre **dois cenários distintos** (entregue / enfileirado) — ver [Antes de ir para produção §1](#1-️-http-200-não-garante-entrega-ao-mop).

### Resposta — `400 Bad Request` (validação do controller)

```json
{
  "status": "ERROR",
  "error": "Invalid header",
  "details": "Header 'origin' must be either 'client' or 'server'",
  "timestamp": "2026-04-27T11:00:01.234Z"
}
```

### Resposta — `400 Bad Request` (header ausente / JSON ilegível)

> ⚠️ Formato **diferente** do anterior — seu cliente precisa tratar os dois.

```json
[
  "Missing required header: clientSSId",
  "Details: Required request header 'clientSSId' for method parameter type String is not present"
]
```

### Resposta — `500 Internal Server Error`

Pode vir em **dois formatos** (controller estruturado **ou** array do `GlobalExceptionHandler`). Sempre logado com stack trace no servidor; não exponha o `details` ao usuário final.

---

## Configuração

### Profiles Spring

| Profile | Arquivo | Quando usar |
|---|---|---|
| (sem) `default` | `application.yml` | Base. Não use diretamente em dev — exige todas as variáveis sem default. |
| `local` | `application-local.yml` | Desenvolvimento local. Sobrescreve porta, fila, intervalo de replay (30 min vs 10 s no base). |

Ative com `SPRING_PROFILES_ACTIVE=local` ou `--spring.profiles.active=local`.

### Variáveis de ambiente — essenciais

> **Para a tabela completa**, com defaults e binding, veja [`docs/VARIAVEIS_DE_AMBIENTE.md`](docs/VARIAVEIS_DE_AMBIENTE.md).

#### Obrigatórias (sem default — aplicação falha sem elas)

| Variável | Descrição |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil Spring (`local` em dev, vazio/`default` em prod). |
| `EXTERNAL_HOST` | Host base do MOP. Preferir URLs completas abaixo. |
| `EXTERNAL_REQUEST_URL` | URL completa do `POST /process`. Produção: `https://mop-server-entrypoint.opinbrasil.com.br/process` · Sandbox: `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process` |
| `EXTERNAL_API_DATA_ANONYMIZATION` | URL completa do `GET` de regras. Produção/sandbox: mesmo host do ambiente + `/anonymization-fields?schema=Consent` |
| `MOP_PAYLOAD_SIGNING_ENABLED` | **`true`** em prod (default no `application.yml` base). Em produção, defina explicitamente junto com `JWS_PRIVATE_KEY`/`JWS_KID`/`JWS_ORG_ID`. |
| `JWS_PRIVATE_KEY` | Chave privada PKCS#8 em PEM. |
| `JWS_KID` | `kid` publicado no JWKS do participante. |
| `JWS_ORG_ID` | `orgId` (UUID) do participante. |

#### RabbitMQ (todas obrigatórias — **sem default**, aplicação falha no boot sem elas)

| Variável | Descrição |
|---|---|
| `RABBITMQ_VALIDATOR_HOST` | Nome herdado do antigo "validator" — **será renomeado**. Em dev local: `localhost`. |
| `RABBITMQ_VALIDATOR_PORT` | Em dev local: `5672`. |
| `RABBITMQ_USERNAME` | Em dev local: `guest`. |
| `RABBITMQ_PASSWORD` | Em dev local: `guest`. |

#### Retry e disponibilidade do MOP

| Variável | Default | Descrição |
|---|---|---|
| `MOP_CLIENT_RETRY_QUEUE` | `mop.client.retry.queue` | Nome da fila AMQP. |
| `MOP_CLIENT_RETRY_REPLAY_ENABLED` | `true` | Liga o replay agendado. |
| `MOP_CLIENT_RETRY_REPLAY_INITIAL_DELAY_MS` | `15000` | Atraso da primeira drenagem após o boot. |
| `MOP_CLIENT_RETRY_REPLAY_INTERVAL_MS` | `10000` *(base)* / `1800000` *(local)* | **Diverge entre profiles** — confira o seu. |
| `MOP_CLIENT_RETRY_REPLAY_MAX_PER_TICK` | `25` | Lote por ciclo. |
| `MOP_SERVER_AVAILABILITY_CHECK_ENABLED` | `true` | Sondas HTTP de disponibilidade. |
| `MOP_SERVER_AVAILABILITY_CHECK_INTERVAL_MS` | `30000` | |
| `MOP_SERVER_AVAILABILITY_CONNECT_TIMEOUT_MS` | `3000` | |
| `MOP_SERVER_AVAILABILITY_READ_TIMEOUT_MS` | `5000` | |

### Precedência das URLs do MOP

```
1º) EXTERNAL_REQUEST_URL                      (vence tudo)
2º) EXTERNAL_REQUEST_HOST + EXTERNAL_REQUEST_PATH
3º) EXTERNAL_HOST          + /process
4º) external.server.request.url               (LEGADO — será removido)
```

---

## Segurança e assinatura JWS

Quando `MOP_PAYLOAD_SIGNING_ENABLED=true`, o gateway assina o **payload final enviado ao MOP** como um JWT compacto, com:

| Item | Valor |
|---|---|
| Algoritmo | `PS256` (RSA-PSS / SHA-256) |
| Header `kid` | `JWS_KID` (deve casar com chave publicada no JWKS do participante) |
| Claim `orgId` | `JWS_ORG_ID` |
| Body assinado | JSON serializado do `MessageDTO` (inclui `trace`, headers, payload anonimizado) |

**Boas práticas operacionais:**

- Rotacione a chave a cada N meses, **publicando a nova no JWKS antes** de mudar o `JWS_KID` do gateway.
- Mantenha pelo menos uma chave antiga no JWKS por uma janela de tolerância (~24 h) para mensagens drenadas da fila de retry.
- Nunca logue o conteúdo de `JWS_PRIVATE_KEY`. Mascarar em qualquer APM/logger.

---

## Observabilidade

### Endpoints do Actuator

Todos sob `/v1/anonymize/actuator/*`:

| Endpoint | Uso | Exemplo |
|---|---|---|
| `/health` | Liveness/readiness — inclui `circuitBreakers`, `rabbit`, `diskSpace`. | `curl http://localhost:8080/v1/anonymize/actuator/health` |
| `/health/circuitBreakers` | Estado dos circuitos `mopAnonymizationConfig` e `mopProcessEndpoint`. | — |
| `/metrics` | Métricas Micrometer (HTTP, JVM, RabbitMQ, Resilience4j). | `/metrics/resilience4j.circuitbreaker.state` |
| `/info` | Metadados da aplicação. | — |

> [!WARNING]
> No `application.yml` base, `management.endpoints.web.exposure.include` está como `"*"` (todos expostos). Em produção, restrinja via `MANAGEMENT_ENDPOINTS_INCLUDE=health,info,metrics,prometheus` e proteja por rede/auth.

### Logs estruturados a monitorar

| Padrão de log | Significado | Ação |
|---|---|---|
| `Payload successfully processed. clientSSId=... correlationId=...` | Entrega ao MOP confirmada. | OK. |
| `[MOP retry] Client received HTTP 200; body sent to retry queue` | Mensagem **enfileirada**, não entregue. | Alertar se taxa elevada. |
| `Header validation failed: ...` | `400` por validação. | Sem ação se baixa frequência. |
| `Failed to process JSON: ...` | `400` por JSON ilegível. | Sem ação se baixa frequência. |
| `Unexpected error processing request: ...` | `500`. | **Alertar imediatamente.** |

---

## Solução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| App falha no boot: `MOP_PAYLOAD_SIGNING_ENABLED=true requires ...` | Assinatura habilitada sem `JWS_PRIVATE_KEY`/`JWS_KID`/`JWS_ORG_ID`. | Defina as três variáveis (produção) ou ajuste `MOP_PAYLOAD_SIGNING_ENABLED=false` (dev). |
| App falha no boot: `JWS_KID must not be blank` | `JWS_KID` ou `JWS_ORG_ID` vazio. | Defina ambas as variáveis. |
| App falha no boot: `Connection refused: amqp://localhost:5672` | RabbitMQ ausente. | `docker compose up -d` ou ajuste `RABBITMQ_VALIDATOR_HOST`. |
| Todas as respostas com log `[MOP retry]` (mesmo retornando 200) | MOP indisponível **ou** circuit `mopProcessEndpoint` aberto. | Cheque `/actuator/health` → seção `circuitBreakers`; valide `EXTERNAL_REQUEST_URL` e conectividade. |
| `401 Unauthorized` recebido do MOP | `kid` desconhecido pelo MOP, ou JWS expirado, ou `orgId` inválido. | Confira que a chave pública está no JWKS do participante e propagada; valide `JWS_KID` e `JWS_ORG_ID`. |
| `400 Header 'origin' must be either 'client' or 'server'` | Header `origin` foi sobrescrito pelo proxy/CDN (colisão com CORS). | Force preservação do header no proxy ou troque de cliente para enviar valor explícito. |
| `400 Header 'operation' must be one of: ...` | Verbo HTTP inválido (ex.: `OPTIONS`). | Use `GET/POST/PUT/PATCH/DELETE`. |
| Resposta `400` veio como **array** em vez de objeto | Erro de header ausente ou body malformado (`GlobalExceptionHandler`). | Seu cliente precisa parsear ambos os formatos — ver [§ Resposta 400](#resposta--400-bad-request-header-ausente--json-ilegível). |
| Replay nunca drena a fila | `MOP_CLIENT_RETRY_REPLAY_ENABLED=false`, ou MOP ainda indisponível, ou intervalo muito grande. | Ver `mop.client.retry.replay.*` e logs do `ClientRetryReplayScheduler`. |

---

## Referências

### Documentação interna

- [`docs/INSTALACAO.md`](docs/INSTALACAO.md) — instalação em Kubernetes via **Helm** (link para o guia oficial no `opin-mop-gateway-pub`).
- [`docs/VARIAVEIS_DE_AMBIENTE.md`](docs/VARIAVEIS_DE_AMBIENTE.md) — todas as variáveis e propriedades, com binding.
- [`docs/REPROCESSAMENTO.md`](docs/REPROCESSAMENTO.md) — fila de retry, caches, retentativas, DLQ.
- [`wiki.md`](wiki.md) — arquitetura interna.
- [`src/main/resources/mop-gateway-api-specification.yml`](src/main/resources/mop-gateway-api-specification.yml) · [`swagger.yaml`](src/main/resources/swagger/swagger.yaml).

### Repositórios descontinuados

> Mantidos por histórico. **Não precisam ser implantados.**

- [`mop-client-data-validator-pub`](https://github.com/br-openinsurance/mop-client-data-validator-pub)
- [`opin-mop-client-anonymization-pub`](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub)

### Documentação externa

- [Spring Boot](https://docs.spring.io/spring-boot/documentation.html)
- [Resilience4j](https://resilience4j.readme.io/)
- [RabbitMQ](https://www.rabbitmq.com/documentation.html)
- [Open Insurance Brasil](https://www.gov.br/susep/pt-br/assuntos/open-insurance)
- [RFC 7807 — Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc7807)

---
