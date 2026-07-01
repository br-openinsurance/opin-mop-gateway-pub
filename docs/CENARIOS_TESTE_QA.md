# Cenários de teste — MOP Client Gateway (QA) 

Documento para validação das **principais funcionalidades** do gateway. Cada item traz **cenário**, **passos** e **resultado esperado**.

| Referência | Valor |
|------------|--------|
| Endpoint gateway | `POST /v1/anonymize/data` |
| Health | `GET /v1/anonymize/actuator/health` |
| URL local gateway | `http://localhost:8080` |
| MOP sandbox — process | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process` |
| MOP sandbox — config | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent` |
| MOP produção — process | `https://mop-server-entrypoint.opinbrasil.com.br/process` |
| MOP produção — config | `https://mop-server-entrypoint.opinbrasil.com.br/anonymization-fields?schema=Consent` |
| Filas RabbitMQ (obrigatórias) | `mop.client.retry.queue` (retry) · `mop.client.retry.dlq` (DLQ) — **criar as duas** como duráveis; ver `docs/VARIAVEIS_DE_AMBIENTE.md` |
| Prioridade | **P0** = bloqueante · **P1** = importante · **P2** = complementar |

**Legenda de execução:** ☐ Não testado · ☑ OK · ✗ Falhou

---

## Referência — Headers HTTP (`POST /data`)

Todos os headers abaixo são enviados na requisição `POST /v1/anonymize/data`.  
**Obrigatório** = ausência ou valor inválido gera **HTTP 400**.  
**Opcional** = pode ser omitido; o gateway segue o processamento com valor padrão ou gerado internamente.

### Headers obrigatórios

| Header | Obrigatório | Descrição | Regras de validação | Exemplo |
|--------|:-----------:|-----------|---------------------|---------|
| `X-Correlation-Id` | **Sim** | ID da intenção lógica / correlação (fornecido pelo cliente). | Não nulo; não vazio após trim; ≥ 1 caractere. Recomendado UUID v4. | `f47ac10b-58cc-4372-a567-0e02b2c3d479` |
| `origin` | **Sim** | Origem da chamada no fluxo MOP. | `client` exige `httpType=Request`; `server` exige `httpType=Response` (case-insensitive). | `client` |
| `path` | **Sim** | Rota **concreta** do recurso Open Insurance (`path_MOP` completo). | Não vazio; deve começar com `/open-insurance/`; **não** usar `{consentId}` literal nem só `/consents`. Ver `PATH_MOP_HEADER.md`. | `/open-insurance/consents/v3/consents` |
| `operation` | **Sim** | Verbo HTTP da operação de negócio original. | Um de: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`, `TRACE` (case-insensitive). | `POST` |
| `httpType` | **Sim** | Tipo da mensagem HTTP no fluxo MOP. | Apenas `Request` ou `Response` (case-insensitive). | `Request` |
| `statusCode` | **Condicional** | Código HTTP da mensagem original. | **Opcional** quando `httpType=Request`. **Obrigatório** quando `httpType=Response` (100–599). Com `Response`, use o status da spec (ex.: **201** no POST consents v3). | `201` |
| `clientSSId` | **Não** | Identificador da parte **receptora** (SS). | Opcional; ecoado em `context.clientSSId`. | `RECEPTORA-A` |
| `serverASId` | **Não** | Identificador da parte **transmissora** (AS). | Opcional; ecoado em `context.serverASId`. | `TRANSMISSORA-B` |

**Se faltar qualquer header obrigatório:** HTTP **400** (formato array de strings do Spring, ex.: `Missing required header: ...`).

**Se o valor for inválido** (validado pelo `HeaderValidator`): HTTP **400** com JSON estruturado (`status: ERROR`, `error: Invalid header`, `details: ...`).

---

### Headers opcionais

| Header | Obrigatório | Descrição | Comportamento / regra | Exemplo |
|--------|:-----------:|-----------|----------------------|---------|
| `traceOrigin` | **Não** | Origem do evento de trace (ex.: `CLIENT`, `SERVER`). | Campo `trace.traceOrigin` vazio no `MessageDTO` enviado ao MOP; fluxo segue normalmente. Repassado na fila de retry se informado. | `CLIENT` |
| `X-Mop-Reportid` | **Não** | ID de rastreio MOP (legado/interno). | Gateway **gera** um identificador (`TraceabilityService`). | `mop-report-7f3c9a2b` |

> **`origin` + `httpType` + `statusCode`:** apenas duas combinações válidas para validação OpenAPI:
>
> | `origin` | `httpType` | `statusCode` | Valida |
> |----------|------------|--------------|--------|
> | `client` | `Request` | opcional | **requestBody** |
> | `server` | `Response` | **obrigatório** | **response body** do status na spec |
>
> `statusCode` deve ser o status **da API Open Insurance** (ex.: POST consents v3 sucesso = **201**). Detalhes: [`PATH_MOP_HEADER.md`](PATH_MOP_HEADER.md).

> **`httpType` e `statusCode`:** `httpType` é sempre obrigatório. `statusCode` só é obrigatório quando `httpType=Response`; com `httpType=Request`, pode ser omitido ou informado (se informado, deve ser 100–599).

> **Nota `traceOrigin`:** quando informado, é serializado em `trace.traceOrigin` do `MessageDTO` enviado ao MOP. **Não** aparece na resposta HTTP do gateway (`POST /data`).

---

### Exemplo mínimo (somente obrigatórios)

```http
POST /v1/anonymize/data HTTP/1.1
X-Correlation-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
origin: client
path: /open-insurance/consents/v3/consents
operation: POST
httpType: Request
Content-Type: application/json

{
  "data": {
    "permissions": ["RESOURCES_READ"],
    "loggedUser": {
      "document": { "identification": "11111111111", "rel": "CPF" }
    },
    "expirationDateTime": "2026-12-31T23:59:59Z"
  }
}
```

### Exemplo completo (obrigatórios + opcionais)

```http
POST /v1/anonymize/data HTTP/1.1
X-Correlation-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
origin: client
path: /open-insurance/consents/v3/consents
operation: POST
httpType: Request
clientSSId: RECEPTORA-A
serverASId: TRANSMISSORA-B
traceOrigin: CLIENT
X-Mop-Reportid: mop-report-7f3c9a2b
Content-Type: application/json

{
  "data": {
    "permissions": ["RESOURCES_READ"],
    "loggedUser": {
      "document": { "identification": "11111111111", "rel": "CPF" }
    },
    "expirationDateTime": "2026-12-31T23:59:59Z"
  }
}
```

### Exemplo com `httpType=Response` (`statusCode` obrigatório)

POST consents v3 — resposta de sucesso na spec é **201** (`ResponseConsent`):

```http
POST /v1/anonymize/data HTTP/1.1
X-Correlation-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
origin: server
path: /open-insurance/consents/v3/consents
operation: POST
httpType: Response
statusCode: 201
clientSSId: RECEPTORA-A
serverASId: TRANSMISSORA-B
Content-Type: application/json

{"data":{"consentId":"urn:prudential:C1DD93123","creationDateTime":"2021-05-21T08:30:00Z","status":"AWAITING_AUTHORISATION","statusUpdateDateTime":"2021-05-21T08:30:00Z","permissions":["RESOURCES_READ"],"expirationDateTime":"2021-05-21T08:30:00Z"},"links":{"self":"https://api.organizacao.com.br/open-insurance/consents/v3/consents/urn:prudential:C1DD93123"},"meta":{"totalRecords":1,"totalPages":1}}
```

GET customers — resposta de sucesso com **200**:

```http
POST /v1/anonymize/data HTTP/1.1
X-Correlation-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
origin: server
path: /open-insurance/customers/v2/personal/identifications
operation: GET
httpType: Response
statusCode: 200
Content-Type: application/json

{"data":[],"links":{"self":"..."},"meta":{"totalRecords":0,"totalPages":1}}
```

### Corpo da requisição (body)

| Item | Obrigatório | Descrição |
|------|:-----------:|-----------|
| Body JSON | **Não** | Pode ser omitido ou vazio → tratado como `{}`. |
| Raiz do JSON | — | Deve ser **objeto** `{}`. Array `[...]` na raiz → **HTTP 400**. |

---

## Funcionalidade 1 — Disponibilidade e ambiente

### Cenário 1.1 — Aplicação inicia corretamente (P0)

**Passos**
1. Configurar variáveis (MOP, RabbitMQ, JWS) conforme `docs/VARIAVEIS_DE_AMBIENTE.md`.
2. **Criar as duas filas** no RabbitMQ: `mop.client.retry.queue` e `mop.client.retry.dlq` (duráveis).
3. Subir a aplicação (`SPRING_PROFILES_ACTIVE=local` ou perfil de QA).

**Resultado esperado**
- Log: `Started MopClientApplication`.
- Porta configurada responde (padrão **8080**).
- Sem falha de boot por RabbitMQ ou JWS ausente.

---

### Cenário 1.2 — Health check operacional (P0)

**Passos**
1. `GET http://localhost:8080/v1/anonymize/actuator/health`

**Resultado esperado**
- HTTP **200**.
- `"status": "UP"`.
- Indicadores de `rabbit` e `circuitBreakers` coerentes com o ambiente.

---

### Cenário 1.3 — RabbitMQ disponível (P0)

**Passos**
1. Verificar broker (Docker Compose ou ambiente de QA).
2. Conferir filas `mop.client.retry.queue` e `mop.client.retry.dlq` na UI (porta 15672, se local).

**Resultado esperado**
- RabbitMQ **running**.
- Fila de retry existe e é acessível.

---

## Funcionalidade 2 — Recepção de eventos (`POST /data`)

### Cenário 2.1 — Requisição válida mínima (P0)

**Passos**
1. Enviar `POST /v1/anonymize/data` usando apenas os **headers obrigatórios** (`X-Correlation-Id`, `origin`, `path`, `operation`, `httpType`) e body JSON objeto — ver *Exemplo mínimo* na referência de headers.

**Resultado esperado**
- HTTP **200** (MOP disponível) ou **202** (MOP indisponível).
- Gateway aceita e processa a requisição (sem erro de contrato HTTP).
- Nenhum header **opcional** é exigido.

---

### Cenário 2.2 — Corpo vazio ou ausente (P0)

**Passos**
1. Repetir cenário 2.1 **sem** body ou com body vazio.

**Resultado esperado**
- Body tratado como `{}`.
- HTTP **200** ou **202** (mesmo critério do MOP).

---

### Cenário 2.3 — Rejeição de array na raiz do JSON (P0)

**Passos**
1. Enviar body: `[{"a":1},{"a":2}]` com headers válidos.

**Resultado esperado**
- HTTP **400**.
- `status`: `ERROR`.
- `error`: `Invalid JSON body`.
- `details` informa que só é permitido **um objeto JSON** por requisição (sem batch em array).

---

### Cenário 2.4 — Context-path obrigatório (P1)

**Passos**
1. Chamar `POST http://localhost:8080/data` (sem `/v1/anonymize`).

**Resultado esperado**
- HTTP **404** (recurso não encontrado).

---

## Funcionalidade 3 — Validação de headers obrigatórios

Testa apenas os headers marcados como **obrigatórios** na [Referência — Headers HTTP](#referência--headers-http-post-data).  
Headers **opcionais** (`traceOrigin`, `X-Mop-Reportid`) não devem ser enviados nestes cenários, salvo indicação contrária.

### Cenário 3.1 — Header obrigatório ausente (P0)

**Passos**
1. Omitir `httpType` (ou outro header obrigatório: `X-Correlation-Id`, `origin`, `path`, `operation`) mantendo o restante válido.

**Resultado esperado**
- HTTP **400**.
- Resposta em formato **array de strings** (Spring), ex.: `"Missing required header: httpType"`.

---

### Cenário 3.2 — Correlation ID vazio (P0)

**Passos**
1. Enviar `X-Correlation-Id:` vazio ou apenas espaços.

**Resultado esperado**
- HTTP **400**.
- JSON estruturado: `error=Invalid header`, `details` menciona `correlationId` (sem campo `status` na raiz).

---

### Cenário 3.3 — Origin inválido (P0)

**Passos**
1. Enviar `origin: INVALID`.

**Resultado esperado**
- HTTP **400**.
- `details`: `Header 'origin' must be either 'client' or 'server'`.

---

### Cenário 3.4 — Origin válido (case-insensitive) (P0)

**Passos**
1. Testar `origin: Client`.
2. Testar `origin: SERVER`.

**Resultado esperado**
- Ambos aceitos (HTTP **200** ou **202**).
- Sem erro de validação de header.

---

### Cenário 3.5 — Operation inválida (P0)

**Passos**
1. Enviar `operation: INVALID_METHOD`.

**Resultado esperado**
- HTTP **400**.
- `details` lista métodos válidos: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE.

---

### Cenário 3.6 — Path vazio ou incompleto (P0)

**Passos**
1. Enviar `path` vazio.
2. Enviar `path: /consents` (sem prefixo `/open-insurance/`).

**Resultado esperado**
- HTTP **400** em ambos.
- Mensagem indicando path vazio **ou** que o path deve começar com `/open-insurance/`.

---

### Cenário 3.6b — `clientSSId` / `serverASId` opcionais (P1)

**Passos**
1. Enviar requisição válida **sem** `clientSSId` e `serverASId`.
2. Repetir omitindo apenas um dos dois.

**Resultado esperado**
- HTTP **200** ou **202** (não é erro de header).
- `context.clientSSId` pode refletir fallback de `origin` quando `clientSSId` ausente.

---

### Cenário 3.12 — `origin` / `httpType` inconsistentes (P0)

**Passos**
1. Enviar `origin: client` com `httpType: Response`.
2. Enviar `origin: server` com `httpType: Request`.

**Resultado esperado**
- HTTP **400** em ambos.
- `details` exige `Request` para `client` ou `Response` para `server`.

---

### Cenário 3.13 — Validação OpenAPI com path MOP completo (P0)

**Passos**
1. `origin: client`, `httpType: Request`, `path: /open-insurance/consents/v3/consents`, `operation: POST`, body `CreateConsent` válido.
2. Repetir com `path: /consents` (inválido — deve falhar no header **antes** da validação OpenAPI).

**Resultado esperado**
- Passo 1: HTTP **200** ou **202**; `validations.status`: `"SUCCESS"`.
- Passo 2: HTTP **400** (path deve começar com `/open-insurance/`).

---

### Cenário 3.14 — `statusCode` incorreto em Response (P1)

**Passos**
1. `origin: server`, `httpType: Response`, `statusCode: 200`, POST consents v3, body de sucesso (`data`, `links`).

**Resultado esperado**
- HTTP **200** ou **202** (gateway aceita headers).
- `validations.status`: `"ERROR"` — schema de erro (`errors`) aplicado em vez de `ResponseConsent`.
- Corrigir para `statusCode: 201` → `validations.status`: `"SUCCESS"`.

---

### Cenário 3.7 — `httpType` ausente ou vazio (P0)

**Passos**
1. Omitir o header `httpType` **ou** enviar `httpType:` vazio.

**Resultado esperado**
- HTTP **400** (Spring: header ausente **ou** `HeaderValidator`: `Header 'httpType' must be one of the following values: Request, Response. Received: ''`).

---

### Cenário 3.8 — `httpType` inválido (P0)

**Passos**
1. Enviar `httpType: INVALID` (ou qualquer valor diferente de `Request` / `Response`).

**Resultado esperado**
- HTTP **400**.
- `details`: `Header 'httpType' must be one of the following values: Request, Response. Received: 'INVALID'`.

---

### Cenário 3.9 — `httpType=Response` sem `statusCode` (P0)

**Passos**
1. Enviar `httpType: Response` sem o header `statusCode`.

**Resultado esperado**
- HTTP **400**.
- `details`: `Header 'statusCode' is required when 'httpType' is 'Response'`.

---

### Cenário 3.10 — `httpType=Request` com `statusCode` opcional (P1)

**Passos**
1. Enviar `httpType: Request` **sem** `statusCode`.
2. Repetir com `httpType: Request` e `statusCode: 200`.

**Resultado esperado**
- Ambos aceitos (HTTP **200** ou **202**).

---

### Cenário 3.11 — `statusCode` inválido (P0)

**Passos**
1. Enviar `httpType: Response` e `statusCode: abc`.
2. Enviar `httpType: Request` e `statusCode: 999` (fora do intervalo 100–599).

**Resultado esperado**
- HTTP **400** em ambos.
- `details` menciona código HTTP válido (100–599).

---

## Funcionalidade 4 — Headers opcionais de trace

Testa apenas os headers marcados como **opcionais** na [Referência — Headers HTTP](#referência--headers-http-post-data).  
Em todos os cenários, manter os **headers obrigatórios** preenchidos (`X-Correlation-Id`, `origin`, `path`, `operation`, `httpType`; `statusCode` quando `httpType=Response`).

| Header testado nesta seção | Obrigatório? |
|----------------------------|:------------:|
| `traceOrigin` | Não |
| `X-Mop-Reportid` | Não |

### Cenário 4.1 — Trace opcional informado (P1)

**Passos**
1. Incluir na requisição válida:
   - `traceOrigin: CLIENT`

**Resultado esperado**
- HTTP **200** ou **202**.
- Nenhum erro de validação por headers opcionais.

---

### Cenário 4.2 — Trace opcional ausente (P0)

**Passos**
1. Enviar requisição válida **sem** `traceOrigin`.

**Resultado esperado**
- HTTP **200** ou **202**.
- Fluxo concluído; gateway preenche defaults internos no `MessageDTO` (`step` derivado de path/operation, timestamp do passo = agora).

---

### Cenário 4.3 — Header `traceOrigin` informado (P0) — *nova alteração*

**Passos**
1. Requisição válida com `traceOrigin: CLIENT`.

**Resultado esperado**
- HTTP **200** ou **202**.
- Header aceito; requisição não rejeitada por `traceOrigin`.

---

### Cenário 4.4 — Header `traceOrigin` ausente (P0) — *nova alteração*

**Passos**
1. Requisição válida sem `traceOrigin`.

**Resultado esperado**
- HTTP **200** ou **202**.
- Comportamento idêntico ao cenário 4.2 (campo opcional).

---

### Cenário 4.5 — `traceOrigin` enviado ao MOP no `MessageDTO` (P0) — *nova alteração*

**Pré-condição:** MOP disponível; capacidade de inspecionar payload enviado ao `/process` (log de debug, proxy ou mock).

**Passos**
1. Enviar requisição válida com `traceOrigin: CLIENT`.
2. Verificar JSON do `MessageDTO` (antes da assinatura JWT ou após decode, conforme ambiente).

**Resultado esperado**
- Objeto `trace` contém `"traceOrigin": "CLIENT"`.
- Valor igual ao header enviado.

---

### Cenário 4.6 — `traceOrigin` preservado na fila de retry (P1) — *nova alteração*

**Pré-condição:** MOP indisponível ou circuit breaker aberto.

**Passos**
1. Enviar requisição com `traceOrigin: CLIENT`.
2. Receber HTTP **202**.
3. Inspecionar mensagem em `mop.client.retry.queue`.

**Resultado esperado**
- Payload enfileirado contém `traceOrigin: CLIENT` no snapshot de headers.

---

### Cenário 4.7 — Resposta HTTP não expõe campos de trace internos (P1)

**Passos**
1. Executar cenário 4.1 com sucesso (200 ou 202).
2. Analisar body da resposta.

**Resultado esperado**
- Body contém: `message`, `timestamp`, `context` (`correlationId`, `clientSSId`, `serverASId`), `request` (`path`, `operation`, `header` com headers ecoados), `validations` (`status`, `total`, `pending`) e, em entrega síncrona ao MOP, `response` (`status` + `body` do `/process`).
- **Não** contém `traceOrigin`, `MessageDTO` nem objeto `trace` completo.

---

### Cenário 4.8 — `X-Mop-Reportid` gerado quando ausente (P2)

**Passos**
1. Requisição válida sem `X-Mop-Reportid`.

**Resultado esperado**
- Processamento segue normalmente.
- Gateway gera identificador interno (verificar em logs se necessário).

---

## Funcionalidade 5 — Processamento e entrega ao MOP (HTTP 200)

Fluxo interno: validação → anonimização (GET fields + apply) → montagem `MessageDTO` → POST `/process` (JWT se JWS ativo).

### Cenário 5.1 — Entrega síncrona com sucesso (P0)

**Pré-condição:** MOP sandbox disponível; credenciais JWS válidas.

**Passos**
1. Enviar requisição válida (cenário 2.1).
2. Conferir resposta HTTP e logs.

**Resultado esperado**
- HTTP **200**.
- Body:
  - `message`: `"Request processed successfully. Your data has been received and forwarded to the server."`
  - `context.correlationId` igual ao header `X-Correlation-Id` enviado
  - `context.clientSSId`, `context.serverASId` ecoados dos headers (quando informados)
  - `request.path`, `request.operation` ecoados dos headers
  - `validations.status`: `"SUCCESS"`, `validations.total`: `0`, `validations.pending`: `[]`
  - `response` presente quando o MOP respondeu na entrega síncrona
  - `timestamp` em ISO-8601
- Log: `Payload successfully processed. clientSSId=... correlationId=...`

---

### Cenário 5.2 — Origin `server` (P1)

**Passos**
1. Requisição válida com `origin: server`.

**Resultado esperado**
- HTTP **200** ou **202**.
- Mesma estrutura de resposta; sem erro de header.

---

### Cenário 5.3 — Correlation ID rastreável em logs (P1)

**Passos**
1. Usar `X-Correlation-Id` fixo e conhecido.
2. Buscar valor nos logs da aplicação.

**Resultado esperado**
- Mesmo `correlationId` aparece nas linhas de processamento da requisição.

---

## Funcionalidade 6 — Resiliência e fila de retry (HTTP 202)

### Cenário 6.1 — Enfileiramento quando MOP indisponível (P0)

**Pré-condição:** MOP fora, URL inválida temporária ou circuit `mopProcessEndpoint` aberto.

**Passos**
1. Enviar requisição válida.
2. Verificar resposta e fila RabbitMQ.

**Resultado esperado**
- HTTP **202**.
- Body:
  - `message`: `"Request accepted and queued for later delivery to the server (MOP unavailable)."`
  - `context` com `correlationId`, `clientSSId`, `serverASId`
  - `request` com `path` e `operation`
  - `validations` com `status: SUCCESS`, `total: 0`, `pending: []`
  - **Sem** campo `response`
- Log: `[MOP retry] Client received HTTP 202; body sent to retry queue | correlationId=...`
- Contador da fila `mop.client.retry.queue` **aumenta**.

---

### Cenário 6.2 — Replay automático após MOP voltar (P1)

**Passos**
1. Executar cenário 6.1.
2. Restaurar MOP.
3. Aguardar intervalo de replay (`MOP_CLIENT_RETRY_REPLAY_INTERVAL_MS`).

**Resultado esperado**
- Fila drena (mensagens processadas).
- Logs de reprocessamento conforme `docs/REPROCESSAMENTO.md`.

---

### Cenário 6.3 — 200 e 202 são cenários distintos (P0)

**Passos**
1. Uma requisição com MOP **UP** (esperar 200).
2. Uma requisição com MOP **DOWN** (esperar 202).

**Resultado esperado**
| Situação | HTTP | Campo `message` no body |
|----------|------|-------------------------|
| Entregue ao MOP | **200** | Texto de sucesso síncrono; inclui `response` |
| Apenas enfileirado | **202** | Texto de aceite para fila; **sem** `response` |

- Mensagens de body **diferentes** entre 200 e 202.
- QA **não** deve assumir entrega ao MOP só pelo fato de não haver erro HTTP.

---

## Funcionalidade 7 — Assinatura JWS e segurança

### Cenário 7.1 — Payload assinado aceito pelo MOP (P0)

**Pré-condição:** `MOP_PAYLOAD_SIGNING_ENABLED=true`; `JWS_PRIVATE_KEY`, `JWS_KID`, `JWS_ORG_ID` corretos; `kid` publicado no JWKS.

**Passos**
1. Executar happy path (cenário 5.1).

**Resultado esperado**
- HTTP **200** no gateway.
- MOP aceita envio (sem 401 por assinatura inválida).

---

### Cenário 7.2 — Kid inválido (P1)

**Passos**
1. Configurar `JWS_KID` incorreto.
2. Enviar requisição válida.

**Resultado esperado**
- Falha na entrega ao MOP (401 no upstream).
- Gateway pode responder **202** e enfileirar, conforme política de retry.

---

## Funcionalidade 8 — Erros e formatos de resposta

### Cenário 8.1 — Dois formatos de erro 400 (P1)

**Passos**
1. Provocar erro de header pelo **controller** (`origin` inválido).
2. Provocar erro de header **ausente** (Spring).

**Resultado esperado**
| Origem | Formato da resposta |
|--------|---------------------|
| Controller (`HeaderValidator`) | JSON objeto: `error`, `details`, `timestamp` |
| Spring (header obrigatório ausente) | **Array de strings** |

- Cliente integrador deve tratar **ambos** os formatos.

---

### Cenário 8.2 — Erro interno não expõe stack trace (P1)

**Passos**
1. Provocar falha inesperada no processamento (ambiente de teste).

**Resultado esperado**
- HTTP **500**.
- Body **sem** stack trace nem dados sensíveis.
- Detalhe técnico apenas nos logs do servidor.

---

## Smoke test pós-deploy (regressão mínima)

| # | Cenário | Resultado esperado resumido |
|---|---------|----------------------------|
| 1 | 1.2 Health | `UP` |
| 2 | 2.1 POST válido | 200 ou 202 |
| 3 | 4.3 Com `traceOrigin` | Aceito |
| 4 | 4.5 `traceOrigin` no MessageDTO ao MOP | `trace.traceOrigin` presente |
| 5 | 4.4 Sem `traceOrigin` | Aceito |
| 6 | 2.3 Array JSON | 400 |
| 7 | 3.3 Origin inválido | 400 |
| 8 | 5.1 ou 6.1 | 200 com log de entrega **ou** 202 com fila |

---

## Evidências em caso de falha

1. `correlationId` da requisição  
2. Status HTTP e body completo  
3. Trecho de log com timestamp  
4. Estado da fila `mop.client.retry.queue` (se 202)  
5. `GET /actuator/health` (circuit breakers)

---

## Referências

- [README.md](../README.md)
- [VARIAVEIS_DE_AMBIENTE.md](VARIAVEIS_DE_AMBIENTE.md)
- [REPROCESSAMENTO.md](REPROCESSAMENTO.md)
- [mop-gateway-api-specification.yml](../src/main/resources/mop-gateway-api-specification.yml)
