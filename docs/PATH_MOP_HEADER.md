# Fórmula do header `path` MOP (specs em `swagger/current/`)

Referência para montar o header **`path`** enviado ao `POST /v1/anonymize/data`, a partir de um arquivo OpenAPI em `src/main/resources/swagger/current/`.

---

## Fórmula

```
path_MOP = normalize( basePath + operationPath )
```

Onde:

| Símbolo | Origem no YAML | Descrição |
|---------|----------------|-----------|
| **basePath** | `servers[0].url` | Apenas o **path** da URL (sem host, sem query). Ex.: `https://api.seguro.com.br/open-insurance/consents/v3` → `/open-insurance/consents/v3` |
| **operationPath** | chave em `paths:` | Path relativo da operação. Ex.: `/consents`, `/consents/{consentId}`, `/lead/request` |
| **normalize()** | — | Garante `/` inicial, remove `/` final (exceto raiz), substitui `{param}` por valores reais |

### Concatenação

```
basePath     = /open-insurance/consents/v3
operationPath = /consents
path_MOP     = /open-insurance/consents/v3/consents
```

Regra: **não** duplicar barra entre base e operação (`base` não termina com `/`, `operation` começa com `/`).

---

## Algoritmo (passo a passo)

1. Abra o `.yaml` correto em `swagger/current/` (ex.: `consents_v3.yaml`).
2. Leia **`servers[0].url`** e extraia só o path:
   - `https://host/open-insurance/consents/v3` → `/open-insurance/consents/v3`
3. Localize a operação em **`paths`** (ex.: `POST /consents` → path `/consents`).
4. Se o path tiver parâmetros (`{consentId}`, `{policyId}`, …), **substitua pelo valor real**:
   - `/consents/{consentId}` + `urn:prudential:C1DD93123` → `/consents/urn:prudential:C1DD93123`
5. Una: `path_MOP = basePath + operationPath` (com valores substituídos).
6. Envie no header HTTP:
   ```http
   path: /open-insurance/consents/v3/consents
   operation: POST
   ```

> O header **`operation`** é o verbo HTTP da chamada Open Insurance original (`GET`, `POST`, `PUT`, `DELETE`). Ele **não** entra na fórmula do `path`, mas deve ser coerente com a operação escolhida no YAML.

---

## Exemplos por API

### Consents v2 / v3

| Arquivo | basePath | operationPath | path_MOP |
|---------|----------|---------------|----------|
| `consents_v2.yaml` | `/open-insurance/consents/v2` | `/consents` | `/open-insurance/consents/v2/consents` |
| `consents_v3.yaml` | `/open-insurance/consents/v3` | `/consents` | `/open-insurance/consents/v3/consents` |
| `consents_v3.yaml` | `/open-insurance/consents/v3` | `/consents/urn:prudential:C1DD93123` | `/open-insurance/consents/v3/consents/urn:prudential:C1DD93123` |

### Resources v2 / v3

| Arquivo | basePath | operationPath | path_MOP |
|---------|----------|---------------|----------|
| `resources_v2.yaml` | `/open-insurance/resources/v2` | `/resources` | `/open-insurance/resources/v2/resources` |
| `resources_v3.yaml` | `/open-insurance/resources/v3` | `/resources` | `/open-insurance/resources/v3/resources` |

### Endorsement

| Arquivo | basePath | operationPath | path_MOP |
|---------|----------|---------------|----------|
| `endorsement.yaml` | `/open-insurance/endorsement/v2` | `/request/{consentId}` | `/open-insurance/endorsement/v2/request/urn:...` |

### Cotação Auto

| Arquivo | basePath | operationPath | path_MOP |
|---------|----------|---------------|----------|
| `quote-auto.yaml` | `/open-insurance/quote-auto/v1` | `/lead/request` | `/open-insurance/quote-auto/v1/lead/request` |
| `quote-auto.yaml` | `/open-insurance/quote-auto/v1` | `/request/{consentId}` | `/open-insurance/quote-auto/v1/request/urn:...` |

### Open Data (vários produtos, mesma base)

Vários arquivos compartilham `servers.url` = `.../open-insurance/products-services/v1` e diferenciam pelo path:

| Arquivo | operationPath | path_MOP |
|---------|---------------|----------|
| `business.yaml` | `/business` | `/open-insurance/products-services/v1/business` |
| `person.yaml` | `/person` | `/open-insurance/products-services/v1/person` |
| `rural.yaml` | `/rural` | `/open-insurance/products-services/v1/rural` |

### Dados transacionais (insurance-*)

| Arquivo | basePath (exemplo) | path_MOP (padrão) |
|---------|-------------------|-------------------|
| `insurance-auto.yaml` | `/open-insurance/insurance-auto/v1` | base + path da operação (ex.: `/.../policies/{policyId}/...`) |

---

## Template de requisição MOP

```http
POST /v1/anonymize/data HTTP/1.1
X-Correlation-Id: {uuid}
origin: client
path: {path_MOP}
operation: {GET|POST|PUT|DELETE}
httpType: {Request|Response}
statusCode: {obrigatório quando httpType=Response — use o status da spec OpenAPI, ex.: 201 para POST /consents}
clientSSId: {receptora}
serverASId: {transmissora}
traceOrigin: {opcional, ex.: CLIENT}
Content-Type: application/json

{ ... payload JSON conforme requestBody ou response do spec ... }
```

### Combinação `origin` + `httpType` + `statusCode`

O gateway valida o body JSON conforme a **mensagem HTTP original** da transação Open Insurance. Os três headers trabalham juntos:

| `origin` | `httpType` | `statusCode` | Schema OpenAPI validado | Quem envia o evento |
|----------|------------|--------------|-------------------------|---------------------|
| `client` | `Request` | opcional (100–599) | **requestBody** de `path` + `operation` | Receptora reportando o que **enviou** |
| `server` | `Response` | **obrigatório** (100–599) | **response body** do status na spec | Transmissora reportando o que **respondeu** |

**Únicas combinações aceitas.** Demais pares (`client`+`Response`, `server`+`Request`, `server`+`Response` sem `statusCode`) → **HTTP 400**.

#### `statusCode` com `httpType=Response`

- Deve ser o **status HTTP real da API Open Insurance**, não o status da resposta do gateway MOP.
- O validador escolhe o schema em `responses['{statusCode}']` do YAML. Se o status não existir na operação, pode cair no schema `default` (geralmente `ResponseError` com campo `errors`).
- Exemplo: POST `/open-insurance/consents/v3/consents` — sucesso é **`201`** (`ResponseConsent`), não `200`. Com `statusCode: 200` e body de sucesso (`data`, `links`), a validação falha pedindo `errors`.

#### Exemplos rápidos (consents v3)

**Request (cliente cria consentimento):**

```http
origin: client
httpType: Request
path: /open-insurance/consents/v3/consents
operation: POST
```

Body: schema `CreateConsent`.

**Response (servidor confirma criação):**

```http
origin: server
httpType: Response
statusCode: 201
path: /open-insurance/consents/v3/consents
operation: POST
```

Body: schema `ResponseConsent` (resposta `201`).

### Regras dos headers `httpType` e `statusCode`

| `httpType` | `statusCode` | Comportamento |
|------------|--------------|---------------|
| `Request` | omitido | Aceito. |
| `Request` | informado | Deve ser código HTTP válido (100–599). |
| `Response` | omitido | **HTTP 400** — `statusCode` obrigatório. |
| `Response` | informado | Obrigatório; 100–599; deve existir em `responses` da operação na spec. |

### Regras do header `origin` e acoplamento com `httpType`

| `origin` | `httpType` obrigatório | Validação OpenAPI do body |
|----------|------------------------|---------------------------|
| `client` | `Request` | **requestBody** da operação (`operation` + `path`) |
| `server` | `Response` | **response body** da operação (`statusCode` + `operation` + `path`) |

Combinações inconsistentes (`client` + `Response`, `server` + `Request`) retornam **HTTP 400** pelo `HeaderValidator`.

---

## Erros comuns

| Erro | Causa | Correção |
|------|-------|----------|
| `path not found from` | path_MOP não existe em nenhum yaml de `swagger/current/` | Confira a fórmula `basePath + operationPath`; use o arquivo correto (v2 vs v3) |
| Path com `{consentId}` literal | Placeholder não substituído | Trocar `{consentId}` pelo URN real |
| Só `/consents` no header | Faltou o basePath | Aplicar a fórmula completa; gateway rejeita com HTTP 400 se não começar com `/open-insurance/` |
| `statusCode: 200` em POST consents | Status de sucesso na spec é **201** | Usar `statusCode: 201` com `origin: server` e `httpType: Response` |
| Body com `data`/`links` e erro pedindo `errors` | `statusCode` não casa com schema de sucesso | Conferir `responses` da operação no YAML |
| URL completa no header | Host incluído | Enviar **somente** o path: `/open-insurance/...` |
| Barra final | `/consents/` | Remover barra final (gateway normaliza, mas prefira sem) |
| v2 vs v3 | Arquivo errado | `consents_v3.yaml` → base termina em `/v3`; v2 em `/v2` |

---

## Relação com validação

| Onde validar | Spec a usar | Path na validação |
|--------------|-------------|-------------------|
| **Gateway MOP (runtime)** | `swagger/current/` | path_MOP completo + `operation` + `httpType` (+ `statusCode` se Response) |
| **Local / CI** (openapi4j, Postman) | Arquivo em `swagger/current/` | Path **relativo** do yaml (`/consents`) + método HTTP correto |

### Comportamento do `OpenApiValidationService`

1. No **startup**, `ApplicationStartupListener` chama `loadAllSpecs()` e registra log `[OPENAPI]`; verifica se `/open-insurance/consents/v3/consents` resolve no registry.
2. Resolve `path` MOP → spec em `swagger/current/` via `OpenApiCurrentSpecRegistry` (fórmula `basePath + operationPath`).
3. Usa header **`operation`** (`GET`, `POST`, `PUT`, `DELETE`, …) na validação openapi4j.
4. Usa header **`httpType`** (deve casar com **`origin`**):
   - `origin=client` + `httpType=Request` → valida **requestBody**
   - `origin=server` + `httpType=Response` → valida **response body** (usa `statusCode` do header)
5. O **RequestValidator** do openapi4j recebe o **path MOP completo** (ex.: `/open-insurance/consents/v3/consents`), não apenas o segmento relativo do YAML (`/consents`).
6. Campos `type: string` com `format: double|float|int32|int64` (ex.: `shareholding`) são ajustados no carregamento da spec (`OpenApiSpecCompatibilityPatcher`) para validar como **string** via `pattern`, conforme a spec Open Insurance.
7. Se nenhum spec modular casar, retorna `NOT_FOUND` (`Operation path not found from URL '...'`).

---

## Índice rápido: arquivo → basePath

Consulte sempre `servers[0].url` no arquivo. Referência frequente:

| Padrão no nome do arquivo | basePath típico |
|---------------------------|-----------------|
| `consents_v2.yaml` | `/open-insurance/consents/v2` |
| `consents_v3.yaml` | `/open-insurance/consents/v3` |
| `resources_v2.yaml` | `/open-insurance/resources/v2` |
| `resources_v3.yaml` | `/open-insurance/resources/v3` |
| `quote-*.yaml` | `/open-insurance/quote-{nome}/v1` ou `v2` |
| `insurance-*.yaml` | `/open-insurance/insurance-{nome}/v1` |
| `*-*.yaml` (open data) | `/open-insurance/products-services/v1` |
| `endorsement.yaml` | `/open-insurance/endorsement/v2` |
| `customers.yaml` | `/open-insurance/customers/v1` |
| `channels / intermediary` | `/open-insurance/channels/v1` |

---

## Referências no repositório

- Specs Open Insurance (validação): [`src/main/resources/swagger/current/`](../src/main/resources/swagger/current/)
- Fórmula do header `path`: este documento
- Cenários QA: [`docs/CENARIOS_TESTE_QA.md`](CENARIOS_TESTE_QA.md)
