# Mapeamento `swagger/current/` × Fases Open Insurance

Referência alinhada à [Área do Desenvolvedor — Open Insurance Brasil](https://opinbrasil.atlassian.net/wiki/spaces/RDD/) e aos grupos de endpoints documentados em `ingestion.yaml`.

Implementação em código: `OpenApiSpecPhaseCatalog` + `OpenInsurancePhase`.

---

## Links oficiais

| Fase | Wiki |
|------|------|
| **Fase 1** — Dados Abertos | [Fase 1 - Dados Abertos](https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/753678/Fase+1+-+Dados+Abertos) |
| **Fase 2** — Dados Relacionados à Movimentações | [Fase 2 - Dados Relacionados à Movimentações](https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/786475/Fase+2+-+Dados+Relacionados+Movimenta+es) |
| **Fase 3** — Serviços de Iniciação de Movimentação | [Fase 3 - Serviços de Iniciação de Movimentação](https://opinbrasil.atlassian.net/wiki/spaces/RDD/pages/4391146/Fase+3+-+Servi+os+de+Inicia+o+de+Movimenta+o) |

---

## Fase 1 — Dados Abertos

Canais de atendimento + catálogo aberto de produtos (`products-services`).

| Arquivo `swagger/current/` | API (wiki) | Prefixo MOP típico |
|----------------------------|------------|-------------------|
| `data_channels.yaml` | Data Channels | `/open-insurance/channels/v1/branches`, `/electronic-channels`, `/phone-channels` |
| `intermediary.yaml` | Intermediary | `/open-insurance/channels/v1/intermediary/{countrySubDivision}` |
| `referenced-network.yaml` | Referenced-network | `/open-insurance/channels/v1/referenced-network/...` |
| `assistance-general-assets.yaml` | Assistance-general-assets | `/open-insurance/products-services/v1/assistance-general-assets` |
| `auto-extended-warranty.yaml` | Auto-extended-warranty | `.../auto-extended-warranty` |
| `auto-insurance.yaml` | Auto-insurance | `.../auto-insurance/{vehicleOvernightZipCode}/{fipeCode}/{year}` |
| `business.yaml` | Business | `.../business` |
| `capitalization-title.yaml` | Capitalization-title | `.../capitalization-title` |
| `condominium.yaml` | Condominium | `.../condominium/{commercializationArea}` |
| `cyber-risk.yaml` | Cyber-risk | `.../cyber-risk` |
| `directors-officers-liability.yaml` | Directors-officers-liability | `.../directors-officers-liability` |
| `domestic-credit.yaml` | Domestic-credit | `.../domestic-credit` |
| `engineering.yaml` | Engineering | `.../engineering` |
| `environmental-liability.yaml` | Environmental-liability | `.../environmental-liability` |
| `equipment-breakdown.yaml` | Equipment-breakdown | `.../equipment-breakdown` |
| `errors-omissions-liability.yaml` | Errors-omissions-liability | `.../errors-omissions-liability` |
| `export-credit.yaml` | Export-credit | `.../export-credit` |
| `extended-warranty.yaml` | Extended-warranty | `.../extended-warranty` |
| `financial-risk.yaml` | Financial-risk | `.../financial-risk` |
| `general-liability.yaml` | General-liability | `.../general-liability` |
| `global-banking.yaml` | Global-banking | `.../global-banking` |
| `home-insurance.yaml` | Home-insurance | `.../home-insurance/commercializationArea/...` |
| `housing.yaml` | Housing | `.../housing` |
| `life-pension.yaml` | Life-pension | `.../life-pension` |
| `lost-profit.yaml` | Lost-profit | `.../lost-profit` |
| `named-operational-risks.yaml` | Named-operational-risks | `.../named-operational-risks` |
| `pension-plan.yaml` | Pension-plan | `.../pension-plan` |
| `person.yaml` | Person | `.../person` |
| `private-guarantee.yaml` | Private-guarantee | `.../private-guarantee` |
| `public-guarantee.yaml` | Public-guarantee | `.../public-guarantee` |
| `rent-guarantee.yaml` | Rent-guarantee | `.../rent-guarantee` |
| `rural.yaml` | Rural | `.../rural` |
| `stop-loss.yaml` | Stop-loss | `.../stop-loss` |
| `transport.yaml` | Transport | `.../transport` |

---

## Fase 2 — Dados Relacionados à Movimentações

Dados cadastrais e de contratos (compartilhamento com consentimento).

| Arquivo | API (wiki) | Exemplo path MOP |
|---------|------------|------------------|
| `customers.yaml` | Customers | `/open-insurance/customers/v2/personal/identifications` |
| `insurance-acceptance-and-branches-abroad.yaml` | Acceptance and Branches Abroad | `.../insurance-acceptance-and-branches-abroad/v2/.../policy-info` |
| `insurance-auto.yaml` | Auto | `.../insurance-auto/v1/.../premium` |
| `insurance-capitalization-title.yaml` | Capitalization Title | `.../insurance-capitalization-title/v1/.../plans` |
| `insurance-financial-assistance.yaml` | Financial Assistance | `.../insurance-financial-assistance/v1/.../contracts` |
| `insurance-financial-risk.yaml` | Financial Risk | `.../insurance-financial-risk/v2/.../claim` |
| `insurance-housing.yaml` | Housing | `.../insurance-housing/v1/.../policy-info` |
| `insurance-life-pension.yaml` | Life Pension | `.../insurance-life-pension/v1/.../movements` |
| `insurance-patrimonial.yaml` | Patrimonial | `.../insurance-patrimonial/v2/.../premium` |
| `insurance-pension-plan.yaml` | Pension Plan | `.../insurance-pension-plan/v1/.../withdrawals` |
| `insurance-person.yaml` | Person | `.../insurance-person/v1/.../claim` |
| `insurance-responsibility.yaml` | Responsibility | `.../insurance-responsibility/v2/.../policy-info` |
| `insurance-rural.yaml` | Rural | `.../insurance-rural/v1/.../policy-info` |
| `insurance-transport.yaml` | Transport | `.../insurance-transport/v2/.../premium` |

---

## Fases 2 e 3 — Transversal

| Arquivo | API | Exemplo path MOP |
|---------|-----|------------------|
| `consents_v2.yaml` | Consents v2 | `/open-insurance/consents/v2/consents` |
| `consents_v3.yaml` | Consents v3 | `/open-insurance/consents/v3/consents/{consentId}` |
| `resources_v2.yaml` | Resources v2 | `/open-insurance/resources/v2/resources` |
| `resources_v3.yaml` | Resources v3 | `/open-insurance/resources/v3/resources` |

---

## Fase 3 — Serviços de Iniciação de Movimentação

Cotação, contratação, sinistro, endosso e webhooks.

| Arquivo | API (wiki) | Exemplo path MOP |
|---------|------------|------------------|
| `claim-notification.yaml` | Claim Notification | `/open-insurance/claim-notification/v2/request/damage/{consentId}` |
| `dynamic-fields.yaml` | Dynamic Fields | `/open-insurance/dynamic-fields/v1/...` |
| `endorsement.yaml` | Endorsement | `/open-insurance/endorsement/v1/request/{consentId}` |
| `quote-patrimonial.yaml` | Quote Patrimonial | `/open-insurance/quote-patrimonial/v2/home/request` |
| `quote-acceptance-and-branches-abroad.yaml` | Quote Acceptance And Branches Abroad | `.../quote-acceptance-and-branches-abroad/v2/lead/request` |
| `quote-auto.yaml` | Quote Auto | `.../quote-auto/v1/request/{consentId}` |
| `quote-financial-risk.yaml` | Quote Financial Risk | `.../quote-financial-risk/v2/lead/request` |
| `quote-housing.yaml` | Quote Housing | `.../quote-housing/v1/lead/request` |
| `quote-responsibility.yaml` | Quote Responsibility | `.../quote-responsibility/v2/lead/request` |
| `quote-rural.yaml` | Quote Rural | `.../quote-rural/v1/lead/request` |
| `quote-transport.yaml` | Quote Transport | `.../quote-transport/v1/lead/request` |
| `quote-capitalization-title.yaml` | API de Capitalização - Contratação | `.../quote-capitalization-title/v1/request` |
| `quote-person.yaml` | API de Pessoas - Contratação | `.../quote-person/v1/life/request` |
| `quote-life-pension.yaml` | API de Previdência - Contratação | `/open-insurance/contract-life-pension/v1/request` |
| `quote-life-pension-withdrawal.yaml` | API Resgate – Previdência e Capitalização | `/open-insurance/withdrawal/v1/pension/request` |
| `webhook.yaml` | Webhook / Notifications | `/open-insurance/webhook/v1/quote/.../quote-status` |

---

## Infraestrutura (fora das fases MOP de validação)

| Arquivo | Uso |
|---------|-----|
| `ingestion.yaml` | PCM — ingestão de métricas |
| `consent-funnel-ingestion.yaml` | PCM — funil de consentimento |
| `discovery.yaml` | Diretório Open Insurance |
| `admin_metrics.yaml` | Admin / métricas |

Estes specs **não** correspondem a paths `/open-insurance/...` usados no header `path` do gateway MOP.

---

## Resolução em runtime

```
path recebido → OpenApiSpecPathIndex (basePath + template)
             → OpenApiSpecPhaseCatalog.phaseForFile(arquivo)
             → OpenApiSpecResolution.phase
```

Consulta direta:

```java
OpenInsurancePhase phase = registry.phaseForPath(
    "/open-insurance/insurance-capitalization-title/v1/insurance-capitalization-title/plans");
// → FASE_2
```

Ver também: [`PATH_MOP_HEADER.md`](PATH_MOP_HEADER.md).
