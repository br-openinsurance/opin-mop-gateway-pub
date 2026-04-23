# MOP Client

API HTTP do ecossistema **Open Insurance** que recebe payloads, executa o pipeline interno de processamento e envia o resultado ao **servidor MOP** (POST). Em situações de indisponibilidade do MOP, o serviço pode persistir o trabalho pendente numa **fila RabbitMQ de retry** (com *circuit breaker* e *replay*), sem expor microsserviços separados de validação ou anonimização.

### Unificação (validator/anonymization descontinuados)

Este repositório **(opin-mop-gateway-pub)** já contempla o que antes existia em serviços separados de **validator** e **anonymization**.

- **validator (descontinuado)**: [`br-openinsurance/mop-client-data-validator-pub`](https://github.com/br-openinsurance/mop-client-data-validator-pub)
- **anonymization (descontinuado)**: [`br-openinsurance/opin-mop-client-anonymization-pub`](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub)

**Importante**: Os repositórios **descontinuados** **não são mais necessários** para o **envio das mensagens** (nem para o pipeline de validator/anonymization) no deploy atual — o fluxo foi **unificado** neste MOP Client.

> **Alerta (continuidade/atualização)**  
> Para manter a continuidade, **não é necessário reconfigurar os componentes** (validator/anonymization).  
> Basta **atualizar** o componente **opin-mop-gateway** (imagem/artefato) para a versão mais recente, mantendo as configurações já aplicadas.

---

### Ambiente sandbox OPIN Brasil

O **sandbox** é o ambiente de testes da OPIN Brasil: você usa o mesmo gateway localmente, mas as chamadas que ele faz “para fora” (servidor MOP) passam a ir para os endpoints públicos de sandbox — **não é preciso cadastro nem chave extra** para “ativar” o sandbox; só configurar as URLs.

**Host base**

| | |
|--|--|
| **URL** | **[https://mop-server-entrypoint-sandbox.opinbrasil.com.br](https://mop-server-entrypoint-sandbox.opinbrasil.com.br)** |

**O que apontar para onde**

| Uso | Endereço completo (exemplo) |
|-----|-----------------------------|
| Enviar o payload processado ao MOP (POST) | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process` |
| Buscar as regras de campos (GET) | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent` |

**Como configurar na prática**

1. Configure as duas URLs com os **valores exatos** do sandbox:
   - **POST (/process)**:
     - variável: `EXTERNAL_REQUEST_URL=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process`
     - ou propriedade: `external.server.request.url=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process`
   - **GET (regras de campos)**:
     - variável: `EXTERNAL_API_DATA_ANONYMIZATION=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent`
     - ou propriedade: `external.api.data-anonymization=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent`
2. Suba a aplicação — a partir daí, ela passa a consultar o sandbox (GET) e enviar o processamento para o sandbox (POST).

Exemplo (Windows PowerShell):

```bash
set SPRING_PROFILES_ACTIVE=local
set EXTERNAL_REQUEST_URL=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process
set EXTERNAL_API_DATA_ANONYMIZATION=https://mop-server-entrypoint-sandbox.opinbrasil.com.br/anonymization-fields?schema=Consent
mvn spring-boot:run
```

Detalhes de nomes de propriedades e perfis: [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md).

---

## Público-Alvo

Esta documentação é destinada a desenvolvedores e equipes técnicas responsáveis pela integração, operação e manutenção do MOP Client Gateway no ecossistema Open Insurance.

## Índice

- [Sobre o MOP](#sobre-o-mop)
- [Início Rápido](#início-rápido)
- [API](#api)
- [Configuração](#configuração)
- [Execução](#execução)
- [Exemplos](#exemplos)
- [Documentação Completa](#documentação-completa)
- [Referências](#referências)

---

## Sobre o MOP

O **Módulo de Operações OPIN (MOP)** promove transparência e confiabilidade no Open Insurance, facilitando comunicação entre sistemas e qualidade dos dados.

**Objetivos:**

- Facilitar acesso direto à infraestrutura dos participantes
- Eliminar obstáculos na comunicação entre sistemas
- Acelerar implementação e análise de qualidade dos dados
- Prevenir riscos de cibersegurança e fraudes

---

## Início Rápido

### URLs externas (`application.yml`)

| Finalidade | Propriedade principal | Variável de ambiente típica |
|------------|------------------------|-----------------------------|
| POST para o servidor MOP | `external.server.request.url` | `EXTERNAL_REQUEST_URL` (via profile `local`; ver [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md)) |
| GET de regras de campos | `external.api.data-anonymization` | `EXTERNAL_API_DATA_ANONYMIZATION` |

Valores padrão no YAML apontam para o ambiente de desenvolvimento interno; para **sandbox**, use o host da seção acima.

### Pré-requisitos

- Java 17+
- Maven 3.x
- **RabbitMQ** Obrigatório.

### Passos

1. **Subir o RabbitMQ (recomendado localmente):**

```bash
docker-compose up -d
```

2. **Executar a aplicação** (profile `local` aplica `application-local.yml` e facilita sobrescritas por variáveis):

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

3. **Testar o endpoint** (`POST` com context path padrão `/v1/anonymize`):

```bash
curl -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: corr-local-001" \
  -H "origin: client" \
  -H "path: /open-insurance/consents/v2/consents" \
  -H "operation: POST" \
  -H "step: consent-created" \
  -H "dataEventoStep: 2026-02-23T18:44:29.650942812Z" \
  -H "clientSSId: RECEPTORA-A" \
  -H "serverASId: TRANSMISSORA-B" \
  -H "Content-Type: application/json" \
  -d "{}"
```

---

## API

### Endpoint

**POST** `{context-path}/data` — padrão: **`/v1/anonymize/data`**.

O corpo JSON é **opcional**; vazio ou inválido é normalizado conforme o `JsonPayloadParser`.

### Headers obrigatórios

| Header | Descrição | Exemplo |
|--------|-----------|---------|
| `X-Correlation-Id` | ID de correlação informado pelo cliente | `corr-2026-001` |
| `origin` | Origem da chamada | `client` ou `server` |
| `path` | Rota lógica / recurso | `/open-insurance/consents/v2/consents` |
| `operation` | Método HTTP da operação | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, … |
| `step` | Etapa do fluxo no trace | `consent-created` |
| `dataEventoStep` | Data/hora do passo (ISO-8601) | `2026-02-23T18:44:29.650942812Z` |
| `clientSSId` | Identificador SS (receptora) | `RECEPTORA-A` |
| `serverASId` | Identificador AS (transmissora) | `TRANSMISSORA-B` |

### Respostas de sucesso (200)

JSON com `status`, `message`, `correlationId`, `timestamp`, `clientSSId`, `serverASId`, `path`, `operation`. A mensagem de sucesso padrão indica que os dados foram recebidos e encaminhados ao servidor; quando o envio imediato ao MOP não é possível e o payload entra na **fila de retry**, o corpo HTTP permanece de sucesso — detalhes em [docs/REPROCESSAMENTO.md](docs/REPROCESSAMENTO.md).

### Erros

- **400:** cabeçalhos inválidos ou erro de JSON no fluxo do controlador.
- **400:** cabeçalho obrigatório ausente ou corpo ilegível — o `GlobalExceptionHandler` pode devolver **lista de strings** (formato distinto do JSON de sucesso acima).
- **500:** falha inesperada no processamento.

---

## Configuração

### Profiles Spring

| Profile | Arquivo | Descrição |
|---------|---------|-----------|
| `default` | `application.yml` | Configuração base (quando `SPRING_PROFILES_ACTIVE` não define outro perfil). |
| `local` | `application-local.yml` | Desenvolvimento local: porta, context path, URL do POST, fila de retry, logging. |

Ativação do profile `local`:

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Variáveis de ambiente

Tabela completa e notas de *binding*: **[docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md)** (inclui RabbitMQ, `mop.client.retry.*`, disponibilidade do MOP, cache, Resilience4j onde aplicável).

---

## Execução

### Maven

```bash
mvn spring-boot:run
```

### Docker

Use o `Dockerfile` do repositório e injete variáveis de ambiente conforme o ambiente (ver [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md)).

---

## Exemplos

### Corpo vazio

```bash
curl -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: example-corr-id" \
  -H "origin: client" \
  -H "path: /open-insurance/consents/v2/consents" \
  -H "operation: POST" \
  -H "step: consent-created" \
  -H "dataEventoStep: 2026-02-23T18:44:29.650942812Z" \
  -H "clientSSId: RECEPTORA-A" \
  -H "serverASId: TRANSMISSORA-B" \
  -H "Content-Type: application/json"
```

### Payload JSON

```bash
curl -X POST http://localhost:8080/v1/anonymize/data \
  -H "X-Correlation-Id: example-corr-id" \
  -H "origin: server" \
  -H "path: /open-insurance/consents/v2/consents" \
  -H "operation: POST" \
  -H "step: consent-created" \
  -H "dataEventoStep: 2026-02-23T18:44:29.650942812Z" \
  -H "clientSSId: RECEPTORA-A" \
  -H "serverASId: TRANSMISSORA-B" \
  -H "Content-Type: application/json" \
  -d "{\"data\":{\"id\":\"123\"}}"
```

---

## Documentação Completa

- **[Variáveis de ambiente](docs/VARIAVEIS_DE_AMBIENTE.md)** — propriedades e variáveis
- **[Reprocessamento, retry e tempos](docs/REPROCESSAMENTO.md)** — fila de retry, caches e retentativas do cliente
- **[Wiki técnico](wiki.md)** — arquitetura resumida
- **[Notas LZ4](docs/VULNERABILIDADES_LZ4.md)** — histórico de dependências
- Especificações em `src/main/resources/mop-gateway-api-specification.yml` e `src/main/resources/swagger/swagger.yaml`

---

## Referências

- **[Repositório](https://github.com/br-openinsurance/opin-mop-gateway-pub)** — este projeto (nomes antigos como “validator”/“anonymization” como serviços separados referem-se a repositórios legados, não ao deploy atual unificado)
- **Repositórios descontinuados (não necessários no deploy unificado)**:
  - [`br-openinsurance/mop-client-data-validator-pub`](https://github.com/br-openinsurance/mop-client-data-validator-pub)
  - [`br-openinsurance/opin-mop-client-anonymization-pub`](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub)

### Documentação técnica

- [RabbitMQ](https://www.rabbitmq.com/documentation.html)
- [Resilience4j](https://resilience4j.readme.io/)
- [Spring Boot](https://docs.spring.io/spring-boot/documentation.html)
- [Open Insurance Brasil](https://www.gov.br/susep/pt-br/assuntos/open-insurance)
