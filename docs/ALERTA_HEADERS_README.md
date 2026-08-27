# ⚠️ ALERTA — Headers da aplicação × headers do MOP Client × validação por produto

> **Público:** Associadas Participantes do Open Insurance Brasil.  
> **Objetivo:** esclarecer as diferenças entre os headers utilizados nas APIs do Open Insurance (aplicação) e aqueles exigidos pelo MOP Client Gateway (rastreabilidade MOP), evitando interpretações equivocadas e reforçando que cada produto/API possui contrato e regras de validação próprios.  
---

## Resumo em uma frase

**Headers que sua aplicação envia ao parceiro Open Insurance ≠ headers que você envia ao MOP Client Gateway.**  
Além disso, **cada produto** (`consents`, `customers`, `insurance-auto`, `quote-*`, funil PCM, etc.) usa **spec OpenAPI diferente**, **path diferente** e **regras de validação diferentes**.

| # | Nome | Em uma frase | Analogia |
|---|------|--------------|----------|
| **1** | Headers da **aplicação** | Etiquetas da conversa **real** com o parceiro | Crachá de entrada no prédio do parceiro |
| **2** | Headers do **MOP Client** | Etiquetas do **relatório** ao gateway MOP | Ficha de ocorrência na central de monitoramento |
| **3** | Validação **por produto** | “Formulário oficial” que checa o **JSON** do relatório | Gabarito daquele produto/versão/status |

```mermaid
flowchart LR
    subgraph conversa [1. Conversa com o parceiro]
        APP[Sua aplicação] -->|Headers FAPI<br/>Authorization, x-fapi-*| PARCEIRO[API Open Insurance]
    end
    subgraph relatorio [2. Relatório ao MOP]
        APP2[Sua aplicação] -->|Headers MOP<br/>X-Correlation-Id, origin, path...| GATEWAY[MOP Client Gateway]
    end
    subgraph checagem [3. Validação por produto]
        GATEWAY --> SPEC[Spec OpenAPI<br/>do produto indicado no path]
    end
```

---

## 1. Três contextos — não misture

| Contexto | Quem recebe | O que são | Exemplos |
|----------|-------------|-----------|----------|
| **A. APIs Open Insurance (aplicação)** | Outro participante (transmissora/receptora) | Headers **FAPI-BR / Open Insurance** da transação real | `Authorization`, `x-fapi-interaction-id`, `x-fapi-auth-date`, `x-idempotency-key`, `x-customer-user-agent`, `x-fapi-customer-ip-address` |
| **B. MOP Client — rastreio (`POST /data`)** | **Seu** MOP Client Gateway | Headers **MOP de trace** que descrevem *qual* operação você reporta | `X-Correlation-Id`, `origin`, `path`, `operation`, `httpType`, `statusCode` (condicional), opcionais `clientSSId`, `serverASId`, `traceOrigin`, `X-Mop-Reportid` |
| **C. MOP Client — funil PCM (`POST /data-funil-consents`)** | **Seu** MOP Client Gateway | **Sem** headers MOP de trace; IDs vão **no corpo JSON** | `correlationId`, `consentId`, `step`, `clientOrgId`, `clientSSId`, `serverOrgId`, `serverASId`, … |

```mermaid
flowchart TB
    subgraph app [Sua aplicação]
        A1[Chamada Open Insurance<br/>com headers FAPI]
        A2[Report ao MOP Client<br/>com headers MOP]
        A3[Evento funil PCM<br/>JSON no body]
    end
    subgraph ext [Participante Open Insurance]
        API[API consents / customers / insurance-* ...]
    end
    subgraph mop [MOP Client Gateway]
        DATA[POST /data]
        FUNIL[POST /data-funil-consents]
    end
    A1 -->|Authorization, x-fapi-*| API
    A2 -->|X-Correlation-Id, origin, path, ...| DATA
    A3 -->|application/json PCM| FUNIL
```

---

# Sessão 1 — Headers da aplicação (APIs Open Insurance)

## 1.1 O que são

Informações de **segurança e rastreio** da chamada **entre instituições** — quando sua app fala com a API do parceiro (ou o parceiro fala com a sua).

**Quem recebe:** o outro participante Open Insurance — **não** o MOP Client Gateway.

| Header | Para que serve | Analogia |
|--------|----------------|----------|
| `Authorization: Bearer …` | Token OAuth — prova de permissão | Crachá de acesso |
| `x-fapi-interaction-id` | Identifica a interação na API do parceiro | Número do protocolo |
| `x-fapi-auth-date` | Quando a autenticação do usuário ocorreu | Carimbo de autenticação |
| `x-idempotency-key` | Evita processar a mesma operação duas vezes | “Não cobrem de novo” |
| `x-fapi-customer-ip-address` | IP do cliente final (quando exigido) | Origem do usuário |
| `x-customer-user-agent` | App/navegador do cliente (quando exigido) | Qual dispositivo/app |

> **⚠️ ALERTA:** esses headers **não substituem** os headers MOP do `POST /data`.  
> O gateway MOP **não valida** se você enviou `Authorization` ou `x-fapi-interaction-id` — ele valida o **JSON do body** contra a spec indicada por `path`, `operation`, `httpType` e `statusCode`.

> **⚠️ ALERTA:** **não reporte** ao MOP Client chamadas ao **token endpoint OAuth** (`/as/token.oauth2`). Elas **não têm** spec em `swagger/current/` e serão rejeitadas. Reporte a **transação Open Insurance** que usa o token. Ver [`PATH_MOP_HEADER.md`](PATH_MOP_HEADER.md#fora-do-escopo-oauth-e-token-endpoint).

## 1.2 Exemplo completo — criar consentimento **no parceiro** (mundo da aplicação)

Esta chamada **não** vai para o MOP. É a operação real Open Insurance.

```http
POST /open-insurance/consents/v3/consents HTTP/1.1
Host: api.parceiro.com.br
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
x-fapi-interaction-id: 550e8400-e29b-41d4-a716-446655440000
x-fapi-auth-date: x1665159629.88
x-idempotency-key: idem-consents-2026-08-27-001
x-customer-user-agent: OpenInsuranceApp/1.0
x-fapi-customer-ip-address: 220.146.215.155
Content-Type: application/json
Accept: application/json

{
  "data": {
    "permissions": [
      "RESOURCES_READ",
      "CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"
    ],
    "loggedUser": {
      "document": {
        "identification": "11111111111",
        "rel": "CPF"
      }
    },
    "expirationDateTime": "2026-12-31T23:59:59Z"
  }
}
```

Resposta típica do **parceiro** (sucesso — status **201** na spec consents v3):

```http
HTTP/1.1 201 Created
x-fapi-interaction-id: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "data": {
    "consentId": "urn:prudential:C1DD93123",
    "creationDateTime": "2026-08-27T14:30:00Z",
    "status": "AWAITING_AUTHORISATION",
    "statusUpdateDateTime": "2026-08-27T14:30:00Z",
    "permissions": [
      "RESOURCES_READ",
      "CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"
    ],
    "expirationDateTime": "2026-12-31T23:59:59Z"
  },
  "links": {
    "self": "https://api.parceiro.com.br/open-insurance/consents/v3/consents/urn:prudential:C1DD93123"
  },
  "meta": {
    "totalRecords": 1,
    "totalPages": 1
  }
}
```

**Leitura:** aqui valem os headers FAPI. O MOP **ainda não** entrou. O próximo passo (Sessão 2) é **reportar** request e/ou response ao gateway.

## 1.3 Checklist — Sessão 1

- [ ] Sei que esses headers vão para o **parceiro**, não para o MOP.  
- [ ] Sei que o token OAuth é para **chamar a API**, não para **reportar ao MOP**.  
- [ ] Não misturo “crachá do parceiro” com “etiqueta do relatório MOP”.

---

# Sessão 2 — Headers do MOP Client (`POST /data`)

## 2.1 O que são

Etiquetas do **relatório** enviado ao **seu** MOP Client Gateway:

> “Aconteceu esta operação Open Insurance — era **Request** ou **Response**, neste **path**, com este **verbo**, e aqui está o **JSON**.”

**Endpoint:** `POST {context-path}/data` (padrão: `POST /v1/anonymize/data`).

### Obrigatórios

| Header | Função | Regra resumida |
|--------|--------|----------------|
| `X-Correlation-Id` | Correlação lógica do evento | Não vazio; recomendado UUID |
| `origin` | Quem reporta | `client` ou `server` (case-insensitive) |
| `path` | Path **completo** Open Insurance | Deve normalizar para `/open-insurance/...` |
| `operation` | Verbo HTTP original | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, … |
| `httpType` | Tipo da mensagem reportada | `Request` ou `Response` |
| `statusCode` | Status HTTP da mensagem original | **Obrigatório** quando `httpType=Response`; opcional quando `Request` |

### Opcionais (gateway)

| Header | Quando usar |
|--------|-------------|
| `clientSSId` | Fases **2 e 3** — identificador da receptora (SS) |
| `serverASId` | Identificador da transmissora (AS) |
| `traceOrigin` | Origem do trace (ex.: `CLIENT`) |
| `X-Mop-Reportid` | Rastreio interno MOP; gerado se omitido |

### Combinações **válidas** (`origin` + `httpType`)

| `origin` | `httpType` | `statusCode` | O que o gateway valida no body |
|----------|------------|--------------|--------------------------------|
| `client` | `Request` | opcional | **requestBody** da operação (`path` + `operation`) |
| `server` | `Response` | **obrigatório** | **response body** do status informado na spec |

Qualquer outra combinação → **HTTP 400** (`HeaderValidator`).

> **`statusCode` não é o HTTP da resposta do gateway.** É o status da **transação Open Insurance**. Ex.: POST consents v3 sucesso = **`201`**, não `200`.

## 2.2 Exemplo completo A — reportar o **Request** (mesmo consentimento da Sessão 1)

```bash
curl -i -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479" \
  -H "origin: client" \
  -H "httpType: Request" \
  -H "path: /open-insurance/consents/v3/consents" \
  -H "operation: POST" \
  -H "clientSSId: RECEPTORA-A" \
  -H "serverASId: TRANSMISSORA-B" \
  -H "traceOrigin: CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "permissions": [
        "RESOURCES_READ",
        "CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"
      ],
      "loggedUser": {
        "document": {
          "identification": "11111111111",
          "rel": "CPF"
        }
      },
      "expirationDateTime": "2026-12-31T23:59:59Z"
    }
  }'
```

**O que está certo neste exemplo**

| Item | Valor | Por quê |
|------|-------|---------|
| Headers FAPI | **Ausentes** | Não são headers do MOP |
| `origin` + `httpType` | `client` + `Request` | Combinação válida |
| `path` | Completo `/open-insurance/consents/v3/...` | Não só `/consents` |
| Body | Schema do **pedido** (`CreateConsent`) | Casa com Request |

Resposta esperada do gateway (entrega síncrona ao MOP — **200**):

```json
{
  "message": "Request processed successfully. Your data has been received and forwarded to the server.",
  "timestamp": "2026-08-27T14:31:00.123Z",
  "context": {
    "correlationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "clientSSId": "RECEPTORA-A",
    "serverASId": "TRANSMISSORA-B"
  },
  "request": {
    "path": "/open-insurance/consents/v3/consents",
    "operation": "POST",
    "header": {
      "x-correlation-id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "origin": "client",
      "httpType": "Request",
      "path": "/open-insurance/consents/v3/consents",
      "operation": "POST",
      "content-type": "application/json"
    }
  },
  "response": {
    "status": 201,
    "body": {
      "message": "Request dispatched for processing.",
      "status": "success"
    }
  },
  "validations": {
    "status": "SUCCESS",
    "total": 0,
    "pending": []
  }
}
```

## 2.3 Exemplo completo B — reportar a **Response** (consentimento criado, status **201**)

```bash
curl -i -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "origin: server" \
  -H "httpType: Response" \
  -H "statusCode: 201" \
  -H "path: /open-insurance/consents/v3/consents" \
  -H "operation: POST" \
  -H "clientSSId: RECEPTORA-A" \
  -H "serverASId: TRANSMISSORA-B" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "consentId": "urn:prudential:C1DD93123",
      "creationDateTime": "2026-08-27T14:30:00Z",
      "status": "AWAITING_AUTHORISATION",
      "statusUpdateDateTime": "2026-08-27T14:30:00Z",
      "permissions": [
        "RESOURCES_READ",
        "CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"
      ],
      "expirationDateTime": "2026-12-31T23:59:59Z"
    },
    "links": {
      "self": "https://api.parceiro.com.br/open-insurance/consents/v3/consents/urn:prudential:C1DD93123"
    },
    "meta": {
      "totalRecords": 1,
      "totalPages": 1
    }
  }'
```

**Pontos críticos**

- `origin=server` + `httpType=Response` + `statusCode=201` (status da **spec**, não “sempre 200”).  
- Body = schema da **resposta 201** (`ResponseConsent`), não o body do request.  
- Use **outro** `X-Correlation-Id` por intenção lógica distinta (request e response são eventos diferentes no modelo at-least-once).

## 2.4 Exemplo completo C — **errado de propósito** (misturar FAPI com MOP)

```bash
# NÃO FAÇA ISTO — falta origin/path/operation/httpType e usa headers do parceiro
curl -i -X POST http://localhost:8080/v1/anonymize/data \
  -H "Authorization: Bearer eyJhbGciOi..." \
  -H "x-fapi-interaction-id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{"data":{"permissions":["RESOURCES_READ"]}}'
```

**Resultado típico:** HTTP **400** — headers MOP obrigatórios ausentes.  
**Correção:** use o formato dos exemplos A ou B (Sessão 2), não o da Sessão 1.

## 2.5 Diferença Sessão 1 × Sessão 2

| Pergunta | Sessão 1 — Aplicação | Sessão 2 — MOP Client |
|----------|----------------------|------------------------|
| Para quem envio? | Parceiro Open Insurance | Seu MOP Client Gateway |
| Objetivo | Executar a operação real | Reportar/rastrear a operação |
| Headers típicos | `Authorization`, `x-fapi-*` | `X-Correlation-Id`, `origin`, `path`, `operation`, `httpType` |
| O MOP valida esses headers? | Não | Sim — rejeita se faltarem ou incoerentes |
| Preciso dos dois? | Sim, na integração real | Sim, se reporta ao MOP — **em chamadas diferentes** |

## 2.6 Checklist — Sessão 2

- [ ] Endpoint: `POST /v1/anonymize/data`.  
- [ ] Preencho `X-Correlation-Id`, `origin`, `path`, `operation`, `httpType`.  
- [ ] Se Response, mando `statusCode` da **spec**.  
- [ ] Não espero que headers FAPI “sirvam” no lugar dos headers MOP.

Detalhes: [`README.md` — Contrato da API](../README.md#contrato-da-api) · [`CENARIOS_TESTE_QA.md`](CENARIOS_TESTE_QA.md).

---

# Sessão 3 — Validação por produto

## 3.1 O que é

Com a ficha (headers MOP) ok, o gateway pergunta: **este JSON bate com o formulário oficial daquele produto?**

Cada produto tem sua spec em `src/main/resources/swagger/current/`. **Não existe schema genérico único.** O header `path` **escolhe** qual gabarito usar.

| Dimensão | Consents v3 | Customers v2 | Insurance-auto v1 | Funil PCM |
|----------|-------------|--------------|-------------------|-----------|
| Arquivo spec | `consents_v3.yaml` | `customers.yaml` | `insurance-auto.yaml` | `consent-funnel-ingestion.yaml` |
| `path` MOP | `/open-insurance/consents/v3/consents` | `/open-insurance/customers/v2/personal/identifications` | `/open-insurance/insurance-auto/v1/.../premium` | *N/A — sem header `path`* |
| Status de sucesso (ex.) | POST cria → **201** | Varia (ex. GET **200**) | Varia por operação | **202** no gateway (aceite local) |
| Campos do body | `CreateConsent`, `ResponseConsent`, … | Schemas de customers | Apólice/prêmio/sinistro | `eventBody` + `additionalInfo` por `step` |

### Fases Open Insurance (impacto prático)

| Fase | Produtos típicos | `clientSSId` no gateway |
|------|------------------|-------------------------|
| **Fase 1** | `products-services`, `data_channels`, catálogos | Opcional / em geral não aplicável |
| **Fase 2** | `customers`, `insurance-*` | Relevante quando receptora reporta |
| **Fase 3** | `quote-*`, iniciação | Relevante quando receptora reporta |

Mapa: [`SWAGGER_FASES.md`](SWAGGER_FASES.md). Fórmula do `path`: [`PATH_MOP_HEADER.md`](PATH_MOP_HEADER.md).

```
path_MOP = basePath (do YAML) + operationPath (com IDs reais, sem {placeholders})
```

## 3.2 Exemplo completo — **outro produto** (customers × consents)

Mesmo verbo `GET` / ideia de “consulta”, **schema e path diferentes**.

```bash
curl -i -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: c0ffee00-1111-2222-3333-444455556666" \
  -H "origin: server" \
  -H "httpType: Response" \
  -H "statusCode: 200" \
  -H "path: /open-insurance/customers/v2/personal/identifications" \
  -H "operation: GET" \
  -H "clientSSId: RECEPTORA-A" \
  -H "serverASId: TRANSMISSORA-B" \
  -H "Content-Type: application/json" \
  -d '{
    "data": [],
    "links": {
      "self": "https://api.parceiro.com.br/open-insurance/customers/v2/personal/identifications"
    },
    "meta": {
      "totalRecords": 0,
      "totalPages": 1
    }
  }'
```

**Por que não copiar o body de consents aqui?**  
Porque a spec é `customers.yaml`, não `consents_v3.yaml`. Payload válido em um produto **pode falhar** no outro.

## 3.3 Exemplos de falha — validação por produto

### Falha 1 — `statusCode` errado (schema de erro vs sucesso)

```bash
# ERRADO: POST consents com statusCode 200 — a spec de sucesso é 201
curl -i -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: $(uuidgen)" \
  -H "origin: server" \
  -H "httpType: Response" \
  -H "statusCode: 200" \
  -H "path: /open-insurance/consents/v3/consents" \
  -H "operation: POST" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "consentId": "urn:prudential:C1DD93123",
      "creationDateTime": "2026-08-27T14:30:00Z",
      "status": "AWAITING_AUTHORISATION",
      "statusUpdateDateTime": "2026-08-27T14:30:00Z",
      "permissions": ["RESOURCES_READ"],
      "expirationDateTime": "2026-12-31T23:59:59Z"
    },
    "links": { "self": "https://api.exemplo/open-insurance/consents/v3/consents/urn:prudential:C1DD93123" },
    "meta": { "totalRecords": 1, "totalPages": 1 }
  }'
```

**Efeito típico:** o validador aplica schema de **outro** status (ex. erro com `errors`) e `validations` fica com pendências — body de sucesso (`data`/`links`) “não encaixa”.  
**Correção:** `statusCode: 201`.

### Falha 2 — `path` incompleto

```bash
# ERRADO
-H "path: /consents"
# CERTO
-H "path: /open-insurance/consents/v3/consents"
```

**Efeito típico:** `Operation path not found from URL '/consents'`.

### Falha 3 — copiar JSON de um produto para outro

Body de `CreateConsent` enviado com `path` de `customers` → pendências de schema.  
**Correção:** abrir o YAML do produto do `path` e montar o body daquela operação.

## 3.4 Como ler `validations`

| Situação | O que você vê | O que fazer |
|----------|---------------|-------------|
| OpenAPI OK | `validations.status` sucesso, `pending` vazio | Também confira HTTP **200** (com `response`) vs **202** (fila) |
| Pendências | Itens em `pending` | Ajustar body / `path` / `statusCode` e **reenviar** |
| Header inválido | Em geral HTTP **400** | Corrigir Sessão 2 primeiro |

> Headers corretos **não garantem** validação OK. A Sessão 3 olha o **conteúdo** com a régua do **produto**.

## 3.5 As três sessões em uma tabela

| Aspecto | 1. Headers da aplicação | 2. Headers do MOP Client | 3. Validação por produto |
|---------|-------------------------|--------------------------|---------------------------|
| Momento | Chamada ao **parceiro** | Chamada ao **gateway MOP** | **Dentro** do gateway, após headers OK |
| Pergunta | “Posso falar com a API?” | “Descrevi certo o evento?” | “O JSON obedece à spec deste produto?” |
| Exemplos | `Authorization`, `x-fapi-interaction-id` | `origin`, `path`, `httpType` | Schema `consents` ≠ `customers` |
| Se errar | Parceiro rejeita / falha FAPI | Gateway rejeita header (ex.: 400) | `validations` com pendências |
| Analogia | Crachá do prédio | Ficha de ocorrência | Gabarito do formulário |

## 3.6 Roteiro prático

1. **Execute** a operação Open Insurance com headers da **Sessão 1**.  
2. **Monte** o report ao MOP com headers da **Sessão 2** (`path` completo, par `origin`/`httpType` válido).  
3. **Confira** o body contra a spec do **produto** (**Sessão 3**).  
4. **Leia** `validations` e corrija só o pendente.  
5. **Não copie** exemplo de outro produto “porque parece parecido”.

## 3.7 Checklist — Sessão 3

- [ ] Sei qual produto estou reportando.  
- [ ] Meu `path` aponta para esse produto (e versão).  
- [ ] Body é do **request** ou da **response** correta daquela spec.  
- [ ] `statusCode` (se Response) é o da spec.  
- [ ] Passar em um produto **não** prova o outro.

---

## 4. Funil de consentimentos PCM (`POST /data-funil-consents`) — contrato **diferente**

Endpoint: **`POST {context-path}/data-funil-consents`** (padrão: `POST /v1/anonymize/data-funil-consents`).

| Aspecto | `POST /data` (rastreio) | `POST /data-funil-consents` (PCM) |
|---------|-------------------------|-----------------------------------|
| Headers MOP (`origin`, `path`, …) | **Sim — obrigatórios** | **Não utiliza** |
| `Content-Type` | `application/json` | `application/json` (ingresso); envio ao MOP `/metrics` é `application/jwt` |
| Identificadores | Headers + body | **Corpo JSON:** `correlationId`, `consentId`, `step`, org/SS IDs, … |
| Validação OpenAPI | Spec do produto em `swagger/current/` conforme `path` | Spec PCM `consent-funnel-ingestion.yaml` + regras por `step` |
| Fila de retry / `/process` | Sim (fluxo principal) | **Não** — fluxo independente |

> **⚠️ ALERTA:** integrar o funil PCM **não** dispensa os headers MOP se você também reporta via `POST /data`. São **dois produtos distintos** no mesmo gateway.

---

## 5. Matriz rápida — o que enviar em cada integração

| Integração | Endpoint gateway | Headers HTTP | Body |
|------------|------------------|--------------|------|
| Reportar request que **minha app enviou** ao parceiro | `POST /data` | MOP: `origin=client`, `httpType=Request`, `path`, `operation`, `X-Correlation-Id`, … | JSON = **requestBody** da spec daquele `path` |
| Reportar response que **minha API devolveu** ao parceiro | `POST /data` | MOP: `origin=server`, `httpType=Response`, `statusCode`, `path`, `operation`, … | JSON = **response** daquele status na spec |
| Evento do **funil de consentimentos** (PCM) | `POST /data-funil-consents` | Apenas `Content-Type: application/json` | JSON PCM (`consentId`, `step`, `correlationId`, …) |
| Obter token OAuth | — | **Não reportar** ao MOP Client | — |

---

## 6. Erros frequentes (e o que fazer)

| Sintoma | Causa provável | Ação |
|---------|----------------|------|
| Enviei `Authorization` ao MOP e deu erro | Confundiu headers **FAPI** com headers **MOP** | No MOP use `X-Correlation-Id`, `origin`, `path`, … |
| `Operation path not found from URL '/consents'` | `path` incompleto | Use `/open-insurance/consents/v3/consents` |
| Validação pede campo `errors` em body com `data` | `statusCode` errado com `httpType=Response` | Consulte a spec: POST consents = **201** |
| `path not found` … `token.oauth2` | Tentou reportar **OAuth** | Reporte a API Open Insurance **após** obter o token |
| Mesmo payload válido em um produto falha em outro | Specs **diferentes** por produto | Confira o YAML em `swagger/current/` para aquele `path` |
| Funil PCM rejeitado por header ausente | Endpoint errado | Use `/data-funil-consents` — **sem** headers MOP de trace |
| `origin` inválido (`client`/`server`) | Proxy/CDN sobrescreveu header `origin` (colisão CORS) | Audite proxy; preserve headers MOP no caminho |

---

## 7. Checklist para times de seguradoras

- [ ] Time de **aplicação** conhece headers **FAPI/Open Insurance** (Sessão 1).  
- [ ] Time de **integração MOP** conhece headers **MOP Client** (Sessão 2) ou body PCM.  
- [ ] Para **cada produto** integrado, documentamos: `path`, `operation`, `httpType`, `statusCode` e exemplo de body.  
- [ ] Consultamos a spec em `swagger/current/` **do produto**, não copiamos exemplo de outro.  
- [ ] Funil PCM (`/data-funil-consents`) tratado como fluxo **separado** de `POST /data`.  
- [ ] Token OAuth **não** é enviado ao MOP Client.

---

## 8. Referências

| Documento | Conteúdo |
|-----------|----------|
| [`README.md`](../README.md#contrato-da-api) | Contrato HTTP do gateway, headers obrigatórios e exemplos curl |
| [`PATH_MOP_HEADER.md`](PATH_MOP_HEADER.md) | Fórmula do `path`, OAuth fora de escopo, `origin`/`httpType`/`statusCode` |
| [`SWAGGER_FASES.md`](SWAGGER_FASES.md) | Mapeamento produto × fase Open Insurance |
| [`CENARIOS_TESTE_QA.md`](CENARIOS_TESTE_QA.md) | Cenários de teste de headers e validação |
| [`VARIAVEIS_DE_AMBIENTE.md`](VARIAVEIS_DE_AMBIENTE.md) | Variáveis de deploy (URLs MOP, JWS, funil PCM) |
| [`WORKSHOP_SQUAD_HUB_MOP_CLIENT.md`](WORKSHOP_SQUAD_HUB_MOP_CLIENT.md) | Roteiro da apresentação Squad Hub |
| [`mop-gateway-api-specification.yml`](../src/main/resources/mop-gateway-api-specification.yml) | OpenAPI do próprio gateway |
| [`swagger/current/`](../src/main/resources/swagger/current/) | Specs usadas na validação — **uma por produto** |

---

> **Dúvidas ou divergência entre ambientes?** Valide sempre contra a **spec do produto** e o **endpoint correto** (`/data` vs `/data-funil-consents`). O MOP Client **não unifica** validações entre produtos — cada API Open Insurance e cada canal PCM trazem regras próprias.
