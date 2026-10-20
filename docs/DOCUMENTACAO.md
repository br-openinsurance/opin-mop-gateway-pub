# Funil de consentimentos (PCM) — documentação

- **Público:** associadas participantes do Open Insurance Brasil.
- **Canal:** `POST /v1/data-funil-consents`
- **Contrato JSON:** [`consent-funnel.yml`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/src/main/resources/swagger/consent-funnel/consent-funnel.yml) — schema `ConsentCreated`
- **Collection:** [`06-funil-consents-por-step.postman_collection.json`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/docs/collections-apresentacao/06-funil-consents-por-step.postman_collection.json)

## Ambiente sandbox — destino das métricas

O gateway **não** envia o funil para o `POST /process`. Depois de validar o JSON contra o swagger acima, ele POSTa o JWT em **`MOP_CONSENT_FUNNEL_METRICS_URL`**.

**Host sandbox OPIN:** `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/`

Configure **esta** URL na variável do funil (homologação / sandbox):

```bash
export MOP_CONSENT_FUNNEL_METRICS_URL=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/metrics
```

| Ambiente | Host MOP Server | `MOP_CONSENT_FUNNEL_METRICS_URL` |
|---|---|---|
| **Sandbox** | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/` | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/metrics` |

Credenciais JWS (`JWS_PRIVATE_KEY`, `JWS_KID`, `JWS_ORG_ID`) devem ser as do **mesmo** ambiente (sandbox). Propriedade Spring: `mop.endpoints.consent-funnel-metrics.url`. Liga/desliga: `MOP_CONSENT_FUNNEL_METRICS_ENABLED` (padrão `true`).

Este documento descreve o ingresso de eventos do **funil de consentimentos** no MOP Client. Não usa headers de rastreio (`origin`, `path`, `httpType`). Os identificadores vão **no JSON**.

---

## 1. O que é

O participante reporta cada etapa do consentimento (criação, autenticação, autorização, uso do recurso, revogação, expiração) em um canal próprio, separado do rastreio `POST /v1/anonymize/data`.

O gateway confere se o evento está completo e coerente com o `step`, aceita o envio e encaminha a métrica ao ambiente central (JWT PS256).

| | Rastreio MOP | Funil PCM |
|---|---|---|
| Endpoint | `POST /v1/anonymize/data` | `POST /v1/data-funil-consents` |
| Headers MOP | Obrigatórios | **Não usa** |
| `origin` / `httpType` | Quatro pares: `client`/`server` × `Request`/`Response` (mesmo `X-Correlation-Id`) | `origin` no JSON (`CLIENT` ou `SERVER`); sem `httpType` |
| Body | JSON da API Open Insurance | JSON PCM (`eventBody`) |
| Fila de retry / `/process` | Sim | **Não** |
| HTTP de aceite | 200 ou 202 (retry) | **202** |

```mermaid
flowchart LR
    APP[Aplicação da associada] -->|JSON PCM<br/>Content-Type application/json| GW[POST /v1/data-funil-consents]
    GW -->|valida eventBody + regras do step| ACC[HTTP 202 ACCEPTED]
    GW -->|JWT PS256| PCM[Métricas PCM]
```

---

## 2. Como chamar

```http
POST /v1/data-funil-consents HTTP/1.1
Host: {gateway}
Content-Type: application/json
```

Não envie `X-Correlation-Id`, `origin`, `path`, `operation` nem `httpType`. O `correlationId` está **no body**.

| HTTP | Quando |
|---|---|
| **202** | Evento aceito (`status: ACCEPTED`) e métrica entregue |
| **400** | JSON inválido ou `additionalInfo` incompleto para o `step` |
| **502** | Evento válido, mas a métrica PCM não pôde ser entregue |

Exemplo de aceite:

```json
{
  "status": "ACCEPTED",
  "correlationId": "577869e5-4c63-4b19-9235-a18d22c80986",
  "consentId": "urn:bancoex:C1DD33123",
  "step": "consent-created",
  "receivedAt": "2026-10-14T19:34:38"
}
```

---

## 3. Exemplo (único payload)

Toda a documentação e a collection usam **este** JSON. Em cada step muda **somente** o campo `step`.

```json
{
  "consentId": "urn:bancoex:C1DD33123",
  "step": "consent-created",
  "origin": "CLIENT",
  "correlationId": "577869e5-4c63-4b19-9235-a18d22c80986",
  "additionalInfo": {
    "consent-user": "invalid-credentials",
    "authentication-failure-reason": "invalid-credentials",
    "user-redirected-back-status": "success",
    "token-kind": "consent-token",
    "rejected-by": "user",
    "revoked-by": "user",
    "expired-by": "authorization-timeout"
  },
  "timestamp": "2026-10-14T19:34:38",
  "clientSSId": "2a59c2a3-529f-41c6-97e3-77395e9951ca",
  "serverASId": "f8cd7b48-197d-419b-8680-f42226111b6f",
  "permissions": [
    "CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"
  ]
}
```

| Campo | Obrigatório | Neste exemplo |
|---|---|---|
| `consentId` | **Sim** | `urn:bancoex:C1DD33123` |
| `step` | **Sim** | um dos 14 valores da seção 4 |
| `origin` | **Sim** | `CLIENT` ou `SERVER` |
| `correlationId` | **Sim** | `577869e5-4c63-4b19-9235-a18d22c80986` |
| `timestamp` | **Sim** | `2026-10-14T19:34:38` (`yyyy-MM-dd'T'HH:mm:ss`, sem fração e sem `Z`) |
| `clientSSId` | **Sim** (UUID) | software da receptora |
| `serverASId` | **Sim** (UUID) | authorization server da transmissora |
| `permissions` | **Sim** | `CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ` |
| `additionalInfo` | Não (chaves internas opcionais) | ver seção 5 |

Só esta estrutura é aceita. `additionalInfo` é **opcional** (pode omitir o objeto ou qualquer chave interna). Qualquer outro campo (`orgId`, `clientOrgId`, `serverOrgId`, `iat`, `exp`, …) gera **HTTP 400**. Campo obrigatório ausente ou vazio gera **400** com a mensagem *The field {name} is required and cannot be empty*. Quando `additionalInfo` vier, ele **é reenviado** ao PCM.

---

## 4. Os 14 steps

Cada linha é um `POST` na collection, com o JSON da seção 3 e o `step` da coluna.

| # | `step` | Quem reporta (spec PCM) | `additionalInfo` exigido |
|---|---|---|---|
| 01 | `consent-created` | Cliente e servidor | `consent-user` |
| 02 | `user-redirected` | Cliente e servidor | — |
| 03 | `user-authentication-failed` | Servidor | `authentication-failure-reason` |
| 04 | `user-authenticated` | Servidor | — |
| 05 | `consent-authorized` | Servidor | — |
| 06 | `consent-rejected` | Servidor | `rejected-by` |
| 07 | `authorization-code-created` | Servidor | — |
| 08 | `user-redirected-back` | Cliente e servidor | `user-redirected-back-status` |
| 09 | `consent-token-generated` | Servidor | — |
| 10 | `consent-token-received` | Cliente | — |
| 11 | `refresh-token-used` | Cliente e servidor | — |
| 12 | `resource-accessed` | Cliente e servidor | `token-kind` |
| 13 | `consent-revoked` | Cliente e servidor | `revoked-by` |
| 14 | `consent-expired` | Cliente e servidor | `expired-by` |

O gateway **não impede** o cliente de enviar um step listado só no servidor (e o contrário). A coluna “quem reporta” é a classificação da spec PCM.

---

## 5. `additionalInfo` por step

O objeto pode trazer todas as chaves (como no exemplo). Nesta spec **não há enum** nem chave obrigatória por step.

| Chave | Obrigatória quando `step` é | Valores aceitos |
|---|---|---|
| `consent-user` | `consent-created` | `user`, `non-user` |
| `authentication-failure-reason` | `user-authentication-failed` | `invalid-credentials`, `invalid-mfa`, `other` |
| `user-redirected-back-status` | `user-redirected-back` | `success`, `failure` |
| `token-kind` | `resource-accessed` | `consent-token`, `client-credential` |
| `rejected-by` | `consent-rejected` | `user`, `system` |
| `revoked-by` | `consent-revoked` | `user`, `system` |
| `expired-by` | `consent-expired` | `authorization-timeout`, `max-date-reached` |

HTTP **400** se faltar campo obrigatório, se o tipo não casar com a spec ou se o JSON tiver campo fora desta estrutura. `additionalInfo` é opcional e, quando enviado, segue no PCM.

---

## 6. Collection Postman

Arquivo: [`06-funil-consents-por-step.postman_collection.json`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/docs/collections-apresentacao/06-funil-consents-por-step.postman_collection.json)

1. Importe a collection.
2. `baseUrl` padrão (ambiente de desenvolvimento): `http://localhost:8080/v1/anonymize`. URL completa: `POST http://localhost:8080/v1/anonymize/data-funil-consents`.
3. Execute os 14 requests **em ordem**, ou o folder inteiro.

Esperado em cada um: **HTTP 202** quando o JSON passa na spec e a métrica PCM responde; **400** se o enum de `consent-user` for rejeitado; **502** se o PCM estiver fora.

---

## 7. O que este canal não faz

- Não anonimiza campos (SHA-256). Isso é só do rastreio `POST /data`.
- Não usa fila RabbitMQ de retry.
- Não envia ao MOP `/process`.
- Não substitui o reporte da API Consents (`CreateConsent` / `ResponseConsent`). Funil e rastreio convivem.

Referência de implementação: `ConsentFunnelController`, `ConsentFunnelOpenApiValidator`, [`consent-funnel.yml`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/src/main/resources/swagger/consent-funnel/consent-funnel.yml).

---

## 8. Referências

Este documento é o item **11** do [sumário do README](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/README.md#sumário).

- [README — Funil de consentimentos (PCM)](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/README.md#funil-de-consentimentos-pcm--documentação)
- [`ALERTA_HEADERS_README.md`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/docs/ALERTA_HEADERS_README.md) — headers FAPI × MOP × funil PCM
- [`release-notes.md`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/docs/release-notes.md#v1-0-8) — versão atual **1.0.8** (funil sandbox, contrato fechado, Dependabot)
- [`06-funil-consents-por-step.postman_collection.json`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/main/docs/collections-apresentacao/06-funil-consents-por-step.postman_collection.json)