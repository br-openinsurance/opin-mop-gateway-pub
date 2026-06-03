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
| `origin` | **Sim** | Origem da chamada no fluxo MOP. | Apenas `client` ou `server` (case-insensitive). | `client` |
| `path` | **Sim** | Rota lógica do recurso Open Insurance. | Não vazio. | `/open-insurance/consents/v2/consents` |
| `operation` | **Sim** | Verbo HTTP da operação de negócio original. | Um de: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`, `TRACE` (case-insensitive). | `POST` |
| `clientSSId` | **Sim** | Identificador da parte **receptora** (SS). | Não vazio. | `RECEPTORA-A` |
| `serverASId` | **Sim** | Identificador da parte **transmissora** (AS). | Não vazio. | `TRANSMISSORA-B` |

**Se faltar qualquer header obrigatório:** HTTP **400** (formato array de strings do Spring, ex.: `Missing required header: ...`).

**Se o valor for inválido** (validado pelo `HeaderValidator`): HTTP **400** com JSON estruturado (`status: ERROR`, `error: Invalid header`, `details: ...`).

---

### Headers opcionais

| Header | Obrigatório | Descrição | Comportamento quando omitido | Exemplo |
|--------|:-----------:|-----------|------------------------------|---------|
| `step` | **Não** | Nome do passo no fluxo de trace. | Gateway usa valor interno no `MessageDTO` (ex.: `request-received`). | `consent-created` |
| `dataEventoStep` | **Não** | Instantâneo ISO-8601 do evento do passo. | Gateway usa timestamp atual no trace. | `2026-04-27T11:00:00Z` |
| `traceOrigin` | **Não** | Origem do evento de trace (ex.: `CLIENT`, `SERVER`). | Campo `trace.traceOrigin` vazio no `MessageDTO` enviado ao MOP; fluxo segue normalmente. Repassado na fila de retry se informado. | `CLIENT` |
| `X-Mop-Reportid` | **Não** | ID de rastreio MOP (legado/interno). | Gateway **gera** um identificador (`TraceabilityService`). | `mop-report-7f3c9a2b` |

> **Nota `traceOrigin`:** quando informado, é serializado em `trace.traceOrigin` do `MessageDTO` enviado ao MOP. **Não** aparece na resposta HTTP do gateway (`POST /data`).

---

### Exemplo mínimo (somente obrigatórios)

```http
POST /v1/anonymize/data HTTP/1.1
X-Correlation-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
origin: client
path: /open-insurance/consents/v2/consents
operation: POST
clientSSId: RECEPTORA-A
serverASId: TRANSMISSORA-B
Content-Type: application/json

{"data":{"id":"qa-001"}}
```

### Exemplo completo (obrigatórios + opcionais)

```http
POST /v1/anonymize/data HTTP/1.1
X-Correlation-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
origin: client
path: /open-insurance/consents/v2/consents
operation: POST
clientSSId: RECEPTORA-A
serverASId: TRANSMISSORA-B
step: consent-created
dataEventoStep: 2026-04-27T11:00:00Z
traceOrigin: CLIENT
X-Mop-Reportid: mop-report-7f3c9a2b
Content-Type: application/json

{"data":{"id":"qa-001"}}
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
2. Subir a aplicação (`SPRING_PROFILES_ACTIVE=local` ou perfil de QA).

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
2. Conferir fila `mop.client.retry.queue` na UI (porta 15672, se local).

**Resultado esperado**
- RabbitMQ **running**.
- Fila de retry existe e é acessível.

---

## Funcionalidade 2 — Recepção de eventos (`POST /data`)

### Cenário 2.1 — Requisição válida mínima (P0)

**Passos**
1. Enviar `POST /v1/anonymize/data` usando apenas os **6 headers obrigatórios** (ver tabela acima) e body JSON objeto — ver *Exemplo mínimo* na referência de headers.

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
Headers **opcionais** (`step`, `dataEventoStep`, `traceOrigin`, `X-Mop-Reportid`) não devem ser enviados nestes cenários, salvo indicação contrária.

### Cenário 3.1 — Header obrigatório ausente (P0)

**Passos**
1. Omitir `clientSSId` (ou outro obrigatório) mantendo o restante válido.

**Resultado esperado**
- HTTP **400**.
- Resposta em formato **array de strings** (Spring), ex.: `"Missing required header: clientSSId"`.

---

### Cenário 3.2 — Correlation ID vazio (P0)

**Passos**
1. Enviar `X-Correlation-Id:` vazio ou apenas espaços.

**Resultado esperado**
- HTTP **400**.
- JSON estruturado: `status=ERROR`, `error=Invalid header`, `details` menciona `correlationId`.

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

### Cenário 3.6 — Path ou participant IDs vazios (P0)

**Passos**
1. Enviar `path` vazio **ou** `clientSSId` / `serverASId` vazio.

**Resultado esperado**
- HTTP **400**.
- Mensagem indicando qual header não pode ser vazio.

---

## Funcionalidade 4 — Headers opcionais de trace

Testa apenas os headers marcados como **opcionais** na [Referência — Headers HTTP](#referência--headers-http-post-data).  
Em todos os cenários, manter os **6 headers obrigatórios** preenchidos.

| Header testado nesta seção | Obrigatório? |
|----------------------------|:------------:|
| `step` | Não |
| `dataEventoStep` | Não |
| `traceOrigin` | Não |
| `X-Mop-Reportid` | Não |

### Cenário 4.1 — Trace completo informado (P1)

**Passos**
1. Incluir na requisição válida:
   - `step: consent-created`
   - `dataEventoStep: 2026-04-27T11:00:00Z`
   - `traceOrigin: CLIENT`

**Resultado esperado**
- HTTP **200** ou **202**.
- Nenhum erro de validação por headers opcionais.

---

### Cenário 4.2 — Trace opcional ausente (P0)

**Passos**
1. Enviar requisição válida **sem** `step`, `dataEventoStep` e `traceOrigin`.

**Resultado esperado**
- HTTP **200** ou **202**.
- Fluxo concluído; gateway preenche defaults internos no `MessageDTO` (`step` derivado, timestamp do passo = agora).

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
- Campos `step` e `dataEventoStep` também preservados, se enviados.

---

### Cenário 4.7 — Resposta HTTP não expõe campos de trace internos (P1)

**Passos**
1. Executar cenário 4.1 com sucesso (200 ou 202).
2. Analisar body da resposta.

**Resultado esperado**
- Body contém apenas: `status`, `message`, `correlationId`, `timestamp`, `clientSSId`, `serverASId`, `path`, `operation`.
- **Não** contém `traceOrigin`, `step`, `MessageDTO` nem objeto `trace` completo.

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
  - `status`: `"SUCCESS"`
  - `message`: `"Request processed successfully. Your data has been received and forwarded to the server."`
  - `correlationId` igual ao header enviado
  - `clientSSId`, `serverASId`, `path`, `operation` ecoados dos headers
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
  - `status`: `"ACCEPTED"`
  - `message`: `"Request accepted and queued for later delivery to the server (MOP unavailable)."`
  - Metadados (`correlationId`, `clientSSId`, etc.) presentes
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
| Situação | HTTP | `status` no body |
|----------|------|------------------|
| Entregue ao MOP | **200** | `SUCCESS` |
| Apenas enfileirado | **202** | `ACCEPTED` |

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
| Controller (`HeaderValidator`) | JSON objeto: `status`, `error`, `details`, `timestamp` |
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
