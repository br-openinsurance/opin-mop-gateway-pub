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
statusCode: {obrigatório quando httpType=Response, ex.: 200}
clientSSId: {receptora}
serverASId: {transmissora}
traceOrigin: {opcional, ex.: CLIENT}
Content-Type: application/json

{ ... payload JSON conforme requestBody ou response do spec ... }
```

### Regras dos headers `httpType` e `statusCode`

| `httpType` | `statusCode` | Comportamento |
|------------|--------------|---------------|
| `Request` | omitido | Aceito. |
| `Request` | informado | Deve ser código HTTP válido (100–599). |
| `Response` | omitido | **HTTP 400** — `statusCode` obrigatório. |
| `Response` | informado | Obrigatório; 100–599. |

### Regras do header `origin`

| `origin` | Validação OpenAPI do body |
|----------|---------------------------|
| `client` | **Request** da operação (`operation` + `path`) |
| `server` | **Response** da operação (status inferido: 201 POST, 200 GET, 204 DELETE) |

---

## Erros comuns

| Erro | Causa | Correção |
|------|-------|----------|
| `path not found from` | path_MOP não existe em nenhum yaml de `swagger/current/` | Confira a fórmula `basePath + operationPath`; use o arquivo correto (v2 vs v3) |
| Path com `{consentId}` literal | Placeholder não substituído | Trocar `{consentId}` pelo URN real |
| Só `/consents` no header | Faltou o basePath | Aplicar a fórmula completa |
| URL completa no header | Host incluído | Enviar **somente** o path: `/open-insurance/...` |
| Barra final | `/consents/` | Remover barra final (gateway normaliza, mas prefira sem) |
| v2 vs v3 | Arquivo errado | `consents_v3.yaml` → base termina em `/v3`; v2 em `/v2` |

---

## Relação com validação

| Onde validar | Spec a usar | Path na validação |
|--------------|-------------|-------------------|
| **Gateway MOP (runtime)** | `swagger/current/` | path_MOP completo no header; openapi4j usa path relativo + `operation` + `origin` |
| **Local / CI** (openapi4j, Postman) | Arquivo em `swagger/current/` | Path **relativo** do yaml (`/consents`) + método HTTP correto |

### Comportamento do `OpenApiValidationService`

1. Resolve `path` MOP → spec em `swagger/current/` via `OpenApiCurrentSpecRegistry` (fórmula `basePath + operationPath`).
2. Usa header **`operation`** (`GET`, `POST`, `PUT`, `DELETE`, …) na validação openapi4j.
3. Usa header **`origin`**:
   - `client` → valida **requestBody**
   - `server` → valida **response body** (status 201 para POST, 200 para GET, 204 para DELETE)
4. Se nenhum spec modular casar, retorna `NOT_FOUND` (`Operation path not found from URL '...'`).

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
