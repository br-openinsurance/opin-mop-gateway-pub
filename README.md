# Guia de Arquitetura e Configuração do Módulo de Operações OPIN (MOP) — MOP Client

Este documento consolidado descreve a estrutura de pacotes, a convenção de filas do RabbitMQ, os parâmetros de configuração Spring Boot e o processo de execução do **MOP Client**. Use-o como referência rápida para padronizar ambientes de desenvolvimento, homologação e produção.
A motivação para desenvolvimento do **Módulo de
Operações OPIN (MOP)** é a promoção da
transparência e confiabilidade no âmbito do Open
Insurance.
▪ Facilitando o acesso direto à infraestrutura dos
participantes, o desenvolvimento da plataforma
pretende eliminar obstáculos na comunicação e
atuar como um acelerador na implementação e
análise de qualidade dos dados e prevenção à riscos
de cibersegurança e fraudes
---

## 1. Visão Geral da Arquitetura

- A aplicação segue uma arquitetura em camadas, separando responsabilidades de domínio, exposição e infraestrutura.
- A mensageria RabbitMQ promove desacoplamento entre serviços internos e externos.
- Toda a configuração sensível é exposta por variáveis de ambiente, permitindo ajustes por ambiente.

---

## 2. Estrutura de Pacotes

| Camada | Responsabilidades | Subpacotes |
|--------|-------------------|------------|
| `application` | Casos de uso e orquestração de lógica de negócio | `usecase`: regras de aplicação<br>`service`: serviços da aplicação |
| `domain` | Modelagem do núcleo do negócio | `entity`: entidades de negócio<br>`valueobject`: objetos de valor<br>`repository`: interfaces de persistência |
| `infrastructure` | Integrações externas e configurações | `adapter`: adaptadores externos<br>`repository`: impls concretas<br>`config`: beans e ajustes do Spring |
| `interface` | Comunicação com o mundo externo | `controller`: endpoints<br>`dto`: contratos de entrada/saída<br>`mapper`: conversões entre DTO e entidade |
| `shared` | Componentes reutilizáveis | `exception`: exceções específicas<br>`util`: funções auxiliares |
| `MainApplication.java` | Classe principal | Ponto de entrada (`main`) da aplicação |

---

## 3. Mensageria RabbitMQ

### 3.1 Filas Mandatórias

Estas filas devem existir em todos os ambientes:

| Fila | Descrição | Produtor | Consumidor |
|------|-----------|----------|------------|
| `data.anonymization.input.queue` | Recebe dados brutos para anonimização | `open-insurance-mop-gateway` | `open-insurance-mop-client-anonymization` |
| `data.anonymization.output.queue` | Armazena dados já anonimizados para processamento ou armazenamento | `open-insurance-mop-client-anonymization` | `open-insurance-mop-validator` |
| `data.validator.input.queue` | Fila intermediária para payloads recebidos pelo gateway que são validados | `open-insurance-mop-validator` | `open-insurance-mop-gateway` |

### 3.2 Diretrizes de Configuração

- **Durável** (`durable=true`) para sobreviver a reinícios.
- **Auto-delete** desativado.
- **Dead Letter Queue** configurada quando aplicável (ex.: `data.anonymization.input.dlq`).
- **Formato de mensagem** em JSON com campos mínimos `id`, `timestamp`, `payload`, `requestId`.
- **Segurança**: TLS, autenticação e autorização habilitadas.
- **Monitoramento**: acompanhar throughput, latência e falhas por fila.
- **Nomenclatura**: letras minúsculas e pontos (`.`) como separadores; evite nomes genéricos.

---

## 4. Configuração Spring Boot

### 4.0 Arquivos de Configuração

A aplicação utiliza dois arquivos de configuração principais:

- **`application.yml`**: Arquivo base para ambientes de produção/staging. **Todas as variáveis de ambiente são obrigatórias e não possuem valores padrão.** Este arquivo deve ser usado em containers Docker, Kubernetes e ambientes de produção.

- **`application_local.yml`**: Arquivo para desenvolvimento local. Contém valores padrão para todas as variáveis, permitindo executar a aplicação localmente sem configurar variáveis de ambiente. Este arquivo é ativado automaticamente quando o perfil `local` está ativo.

**Importante:** Em ambientes de produção, todas as variáveis de ambiente listadas na seção 5.1 devem ser configuradas obrigatoriamente, pois o `application.yml` não possui valores padrão.

### 4.1 Aplicação
```yaml
spring.application.name: ${SPRING_APPLICATION_NAME:mop-client-gateway}
```
O valor à direita do `:` indica o padrão utilizado quando `SPRING_APPLICATION_NAME` não é informado; se nenhum valor for definido, a aplicação sobe como `mop-client-gateway`.

### 4.2 Servidor
```yaml
server:
  port: ${SERVER_PORT}
  servlet:
    context-path: ${SERVER_CONTEXT_PATH}
```
**Variáveis de ambiente obrigatórias:**
- `SERVER_PORT`: Porta do servidor (ex: `8080`)
- `SERVER_CONTEXT_PATH`: Context path da aplicação (ex: `/v1/anonymize`)

**Nota:** No arquivo `application.yml` (base), estas variáveis são obrigatórias. Valores padrão estão disponíveis apenas em `application_local.yml` para desenvolvimento local.

### 4.3 RabbitMQ
```yaml
# Unified Spring AMQP Configuration
spring.rabbitmq:
  # Connection settings (obrigatórias)
  host: ${RABBITMQ_HOST}
  port: ${RABBITMQ_PORT}
  username: ${RABBITMQ_USERNAME}
  password: ${RABBITMQ_PASSWORD}
  
  # Listener configuration
  listener:
    simple:
      acknowledge-mode: auto
      concurrency: ${RABBITMQ_CONCURRENCY:1}
      max-concurrency: ${RABBITMQ_MAX_CONCURRENCY:5}
      prefetch: ${RABBITMQ_PREFETCH:10}
  
  # Application-specific queues (obrigatórias)
  queues:
    validator:
      name: ${RABBITMQ_QUEUE_VALIDATOR}
    output:
      name: ${RABBITMQ_QUEUE_OUTPUT}
  
  # Retry configuration
  retry:
    maxAttempts: ${RABBITMQ_RETRY_MAX_ATTEMPTS:5}
    backoff: ${RABBITMQ_RETRY_BACKOFF:2000}
    enablesTransactionSupport: true
```

**Variáveis de ambiente obrigatórias:**
- `RABBITMQ_HOST`: Host do RabbitMQ
- `RABBITMQ_PORT`: Porta do RabbitMQ
- `RABBITMQ_USERNAME`: Usuário do RabbitMQ
- `RABBITMQ_PASSWORD`: Senha do RabbitMQ
- `RABBITMQ_QUEUE_VALIDATOR`: Nome da fila de validação
- `RABBITMQ_QUEUE_OUTPUT`: Nome da fila de saída

**Variáveis de ambiente opcionais:**
- `RABBITMQ_CONCURRENCY`: Número de consumidores concorrentes (padrão: `1`)
- `RABBITMQ_MAX_CONCURRENCY`: Máximo de consumidores concorrentes (padrão: `5`)
- `RABBITMQ_PREFETCH`: Contagem de prefetch (padrão: `10`)
- `RABBITMQ_RETRY_MAX_ATTEMPTS`: Tentativas máximas de retry (padrão: `5`)
- `RABBITMQ_RETRY_BACKOFF`: Tempo de backoff do retry em ms (padrão: `2000`)

**Nota:** No arquivo `application.yml` (base), as variáveis marcadas como obrigatórias não possuem valores padrão. Valores padrão estão disponíveis apenas em `application_local.yml` para desenvolvimento local.

### 4.4 Endpoints de Gerenciamento
```yaml
management:
  endpoints:
    web:
      exposure:
        include: ${MANAGEMENT_ENDPOINTS_INCLUDE}
  endpoint:
    health:
      show-details: ${MANAGEMENT_HEALTH_SHOW_DETAILS}
```

**Variáveis de ambiente obrigatórias:**
- `MANAGEMENT_ENDPOINTS_INCLUDE`: Endpoints do Actuator a expor (ex: `*` para todos)
- `MANAGEMENT_HEALTH_SHOW_DETAILS`: Detalhes do health check (ex: `always`)

**Nota:** No arquivo `application.yml` (base), estas variáveis são obrigatórias. Valores padrão estão disponíveis apenas em `application_local.yml` para desenvolvimento local.

---

## 5. Variáveis de Ambiente

As tabelas abaixo mapeiam as propriedades Spring expostas por cada aplicação para suas respectivas variáveis de ambiente e valores padrão.

### 5.1 Gateway (`open-insurance-mop-gateway`)

| Variável | Propriedade Spring | Obrigatória | Valor padrão (apenas local) |
|----------|--------------------|-------------|----------------------------|
| `SERVER_PORT` | `server.port` | Sim | `8080` (apenas em `application_local.yml`) |
| `SERVER_CONTEXT_PATH` | `server.servlet.context-path` | Sim | `/v1/anonymize` (apenas em `application_local.yml`) |
| `RABBITMQ_HOST` | `spring.rabbitmq.host` | Sim | `localhost` (apenas em `application_local.yml`) |
| `RABBITMQ_PORT` | `spring.rabbitmq.port` | Sim | `5672` (apenas em `application_local.yml`) |
| `RABBITMQ_USERNAME` | `spring.rabbitmq.username` | Sim | `guest` (apenas em `application_local.yml`) |
| `RABBITMQ_PASSWORD` | `spring.rabbitmq.password` | Sim | `guest` (apenas em `application_local.yml`) |
| `RABBITMQ_CONCURRENCY` | `spring.rabbitmq.listener.simple.concurrency` | Não | `1` |
| `RABBITMQ_MAX_CONCURRENCY` | `spring.rabbitmq.listener.simple.max-concurrency` | Não | `5` |
| `RABBITMQ_PREFETCH` | `spring.rabbitmq.listener.simple.prefetch` | Não | `10` |
| `RABBITMQ_QUEUE_VALIDATOR` | `spring.rabbitmq.queues.validator.name` | Sim | `data.validator.input.queue` (apenas em `application_local.yml`) |
| `RABBITMQ_QUEUE_OUTPUT` | `spring.rabbitmq.queues.output.name` | Sim | `data.anonymization.output.queue` (apenas em `application_local.yml`) |
| `RABBITMQ_RETRY_MAX_ATTEMPTS` | `spring.rabbitmq.retry.maxAttempts` | Não | `5` |
| `RABBITMQ_RETRY_BACKOFF` | `spring.rabbitmq.retry.backoff` | Não | `2000` |
| `EXTERNAL_API_URL` | `external.server.request.url` | Sim | `localhost/process` (apenas em `application_local.yml`) |
| `MANAGEMENT_ENDPOINTS_INCLUDE` | `management.endpoints.web.exposure.include` | Sim | `*` (apenas em `application_local.yml`) |
| `MANAGEMENT_HEALTH_SHOW_DETAILS` | `management.endpoint.health.show-details` | Sim | `always` (apenas em `application_local.yml`) |

**Nota:** No arquivo `application.yml` (base), todas as variáveis são obrigatórias e não possuem valores padrão. Os valores padrão listados acima são apenas para referência do arquivo `application_local.yml`, usado para desenvolvimento local.

### 5.2 Validator (`open-insurance-mop-validator`)

| Variável | Propriedade Spring | Valor padrão |
|----------|--------------------|--------------|
| `SERVER_PORT_VALIDATOR` | `server.port` | `8084` |
| `RABBITMQ_USERNAME` | `rabbitmq.username` | `guest` |
| `RABBITMQ_PASSWORD` | `rabbitmq.password` | `guest` |
| `RABBITMQ_RETRY_MAX_ATTEMPTS` | `rabbitmq.retry.maxAttempts` | `5` |
| `RABBITMQ_RETRY_BACKOFF` | `rabbitmq.retry.backoff` | `2000` |
| `RABBITMQ_ENABLES_TRANSACTION_SUPPORT` | `rabbitmq.retry.enablesTransactionSupport` | `true` |
| `RABBITMQ_VALIDATOR_QUEUE_NAME` | `rabbitmq.validator.queue.name` | `data.validator.input.queue` |
| `RABBITMQ_VALIDATOR_HOST` | `rabbitmq.validator.host` | `localhost` |
| `RABBITMQ_VALIDATOR_PORT` | `rabbitmq.validator.port` | `5672` |
| `RABBITMQ_INPUT_QUEUE_NAME` | `rabbitmq.input.queue.name` | `data.anonymization.input.queue` |
| `RABBITMQ_INPUT_HOST` | `rabbitmq.input.host` | `localhost` |
| `RABBITMQ_INPUT_PORT` | `rabbitmq.input.port` | `5672` |


### 5.3 Anonymization (`open-insurance-mop-anonymization`)

| Variável | Propriedade Spring | Valor padrão                                           |
|----------|--------------------|--------------------------------------------------------|
| `SERVER_PORT_ANONYMIZATION` | `server.port` | `8181`                                                 |
| `EXTERNAL_API_DATA_ANONYMIZATION` | `external.api.data-anonymization` | `http://localhost/anonymization-fields?schema=Consent` |
| `EXTERNAL_REQUEST_URL` | `external.request.url` | `http://localhost/process`                                    |
| `EXTERNAL_REQUEST_HOST` | `external.request.host` | `http://localhost`        |
| `EXTERNAL_REQUEST_PATH` | `external.request.path` | `/process`                                             |
| `EXTERNAL_REQUEST_METHOD` | `external.request.method` | `POST`                                                 |
| `RABBITMQ_USERNAME` | `rabbitmq.username` | `guest`                                                |
| `RABBITMQ_PASSWORD` | `rabbitmq.password` | `guest`                                                |
| `RABBITMQ_INPUT_QUEUE_NAME` | `rabbitmq.input.queue.name` | `data.anonymization.input.queue`                       |
| `RABBITMQ_INPUT_HOST` | `rabbitmq.input.host` | `localhost`                                            |
| `RABBITMQ_INPUT_PORT` | `rabbitmq.input.port` | `5672`                                                 |
| `RABBITMQ_OUTPUT_QUEUE_NAME` | `rabbitmq.output.queue.name` | `data.anonymization.output.queue`                      |
| `RABBITMQ_OUTPUT_HOST` | `rabbitmq.output.host` | `localhost`                                            |
| `RABBITMQ_OUTPUT_PORT` | `rabbitmq.output.port` | `5672`                                                 |
---

## 6. Execução Local

### 6.1 Pré-requisitos

- Java 17
- Maven 3.x
- Docker
- Docker Compose

### 6.2 Configuração do RabbitMQ via Docker Compose

Antes de executar a aplicação localmente, é necessário subir o RabbitMQ utilizando o arquivo `docker-compose.yml` fornecido no projeto.

**Passos para iniciar o RabbitMQ:**

1. Certifique-se de que o Docker está em execução.
2. No diretório raiz do projeto, execute o comando:
   ```bash
   docker-compose up -d
   ```
3. Aguarde alguns segundos para o RabbitMQ inicializar completamente.
4. Verifique se o container está rodando:
   ```bash
   docker ps
   ```
5. Acesse a interface de gerenciamento do RabbitMQ em: `http://localhost:15672`
   - **Usuário:** `guest`
   - **Senha:** `guest`

**Configuração do RabbitMQ:**
- **Porta AMQP:** `5672` (usada pela aplicação)
- **Porta Management UI:** `15672` (interface web)
- **Usuário padrão:** `guest`
- **Senha padrão:** `guest`

**Para parar o RabbitMQ:**
```bash
docker-compose down
```

> **Importante:** O RabbitMQ deve estar em execução antes de iniciar a aplicação Spring Boot, pois ela depende do broker de mensageria para funcionar corretamente.

## 7 Passos via Docker

1. Baixe as imagens obrigatórias (gateway, validator e anonymization):
   ```bash
   docker pull ghcr.io/br-openinsurance-infra/opin-mop-gateway/open-insurance-mop-gateway:develop
   docker pull ghcr.io/br-openinsurance-infra/mop-client-data-validator/open-insurance-mop-validator:develop
   docker pull ghcr.io/br-openinsurance-infra/opin-mop-client-anonymization/open-insurance-mop-anonymization:develop
   ```
2. Execute os containers exportando as variáveis necessárias (exemplo POSIX):

   **Gateway**
   ```bash
   docker run --rm --name mop-gateway \
     -e SERVER_PORT=${SERVER_PORT:-8080} \
     -e SERVER_CONTEXT_PATH=${SERVER_CONTEXT_PATH:-/v1/anonymize} \
     -e RABBITMQ_HOST=${RABBITMQ_HOST:-localhost} \
     -e RABBITMQ_PORT=${RABBITMQ_PORT:-5672} \
     -e RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest} \
     -e RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest} \
     -e RABBITMQ_CONCURRENCY=${RABBITMQ_CONCURRENCY:-1} \
     -e RABBITMQ_MAX_CONCURRENCY=${RABBITMQ_MAX_CONCURRENCY:-5} \
     -e RABBITMQ_PREFETCH=${RABBITMQ_PREFETCH:-10} \
     -e RABBITMQ_QUEUE_VALIDATOR=${RABBITMQ_QUEUE_VALIDATOR:-data.validator.input.queue} \
     -e RABBITMQ_QUEUE_OUTPUT=${RABBITMQ_QUEUE_OUTPUT:-data.anonymization.output.queue} \
     -e RABBITMQ_RETRY_MAX_ATTEMPTS=${RABBITMQ_RETRY_MAX_ATTEMPTS:-5} \
     -e RABBITMQ_RETRY_BACKOFF=${RABBITMQ_RETRY_BACKOFF:-2000} \
     -e EXTERNAL_API_URL=${EXTERNAL_API_URL:-localhost/process} \
     -e MANAGEMENT_ENDPOINTS_INCLUDE=${MANAGEMENT_ENDPOINTS_INCLUDE:-*} \
     -e MANAGEMENT_HEALTH_SHOW_DETAILS=${MANAGEMENT_HEALTH_SHOW_DETAILS:-always} \
     -p ${SERVER_PORT:-8080}:${SERVER_PORT:-8080} \
     ghcr.io/br-openinsurance-infra/opin-mop-gateway/open-insurance-mop-gateway:develop
   ```

   **Validator**
   ```bash
   docker run --rm --name mop-validator \
     -e SERVER_PORT=${SERVER_PORT_VALIDATOR:-8084} \
     -e RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest} \
     -e RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest} \
     -e RABBITMQ_RETRY_MAX_ATTEMPTS=${RABBITMQ_RETRY_MAX_ATTEMPTS:-5} \
     -e RABBITMQ_RETRY_BACKOFF=${RABBITMQ_RETRY_BACKOFF:-2000} \
     -e RABBITMQ_ENABLES_TRANSACTION_SUPPORT=${RABBITMQ_ENABLES_TRANSACTION_SUPPORT:-true} \
     -e RABBITMQ_VALIDATOR_QUEUE_NAME=${RABBITMQ_VALIDATOR_QUEUE_NAME:-data.validator.input.queue} \
     -e RABBITMQ_VALIDATOR_HOST=${RABBITMQ_VALIDATOR_HOST:-localhost} \
     -e RABBITMQ_VALIDATOR_PORT=${RABBITMQ_VALIDATOR_PORT:-5672} \
     -e RABBITMQ_INPUT_QUEUE_NAME=${RABBITMQ_INPUT_QUEUE_NAME:-data.anonymization.input.queue} \
     -e RABBITMQ_INPUT_HOST=${RABBITMQ_INPUT_HOST:-localhost} \
     -e RABBITMQ_INPUT_PORT=${RABBITMQ_INPUT_PORT:-5672} \
     -p ${SERVER_PORT_VALIDATOR:-8084}:${SERVER_PORT_VALIDATOR:-8084} \
     ghcr.io/br-openinsurance-infra/mop-client-data-validator/open-insurance-mop-validator:develop
   ```

   **Anonymization**
   ```bash
   docker run --rm --name mop-anonymization \
     -e SERVER_PORT=${SERVER_PORT_ANONYMIZATION:-8181} \
     -e RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest} \
     -e RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest} \
     -e RABBITMQ_INPUT_QUEUE_NAME=${RABBITMQ_INPUT_QUEUE_NAME:-data.anonymization.input.queue} \
     -e RABBITMQ_INPUT_HOST=${RABBITMQ_INPUT_HOST:-localhost} \
     -e RABBITMQ_INPUT_PORT=${RABBITMQ_INPUT_PORT:-5672} \
     -e RABBITMQ_OUTPUT_QUEUE_NAME=${RABBITMQ_OUTPUT_QUEUE_NAME:-data.anonymization.output.queue} \
     -e RABBITMQ_OUTPUT_HOST=${RABBITMQ_OUTPUT_HOST:-localhost} \
     -e RABBITMQ_OUTPUT_PORT=${RABBITMQ_OUTPUT_PORT:-5672} \
     -e EXTERNAL_API_DATA_ANONYMIZATION=${EXTERNAL_API_DATA_ANONYMIZATION:-http://mop-server-entrypoint-dev.intranet.opinbrasil/anonymization-fields?schema=Consent} \
     -e EXTERNAL_REQUEST_URL=${EXTERNAL_REQUEST_URL:-http://mop-server-entrypoint-dev.intranet.opinbrasil/process} \
     -e EXTERNAL_REQUEST_HOST=${EXTERNAL_REQUEST_HOST:-mop-server-entrypoint-dev.intranet.opinbrasil} \
     -e EXTERNAL_REQUEST_PATH=${EXTERNAL_REQUEST_PATH:-/process} \
     -e EXTERNAL_REQUEST_METHOD=${EXTERNAL_REQUEST_METHOD:-POST} \
     -p ${SERVER_PORT_ANONYMIZATION:-8181}:${SERVER_PORT_ANONYMIZATION:-8181} \
     ghcr.io/br-openinsurance-infra/opin-mop-client-anonymization/open-insurance-mop-anonymization:develop
   ```

   > Em shells Windows substitua `\` por `^`.

### 7.1 Comportamento dos valores padrão no Docker

- A sintaxe `${VARIAVEL:-valorPadrao}` é avaliada pelo shell antes do `docker run`. Se `VARIAVEL` estiver definida no seu terminal, o valor informado é usado; caso contrário, o texto após os dois pontos é enviado para o container.
- Isso significa que você pode omitir qualquer `-e` e confiar no valor padrão mostrado no comando, obtendo o mesmo comportamento descrito na tabela de variáveis de ambiente.
- Para personalizar um valor, defina a variável no shell (ex.: `export SERVER_PORT=9090`) antes de executar o comando ou substitua diretamente o trecho `${VARIAVEL:-valorPadrao}` pelo valor desejado.

## 8. Consulta ao diagrama de arquitetura do cliente

1. Localize o arquivo `docs/assets/mop-client-arquitetura.png`, que contém o diagrama oficial do MOP Client.
2. Abra a imagem para revisar o fluxo ponta a ponta (produtor → broker → consumidor) antes de configurar novas integrações.
3. Caso o arquivo ainda não esteja presente, utilize a imagem compartilhada pela equipe de arquitetura e salve-a com o mesmo nome para manter o documento sincronizado.

### 8.1 Exemplo de requisição via Postman/cURL

Use este passo a passo para validar rapidamente o endpoint `/v1/anonymize/data` depois de subir os containers:

1. Abra o Postman e crie uma requisição `POST` apontando para `http://localhost:8080/v1/anonymize/data`.
2. Na aba **Headers**, inclua:
  - `origin: Sistema` → identifica o sistema originador.
  - `destination: Sistema` → identifica o destino esperado pelo gateway.
  - `path: /open-insurance/consents/v2/consents` → rota alvo que será registrada nos logs.
  - `operation: POST` → método da operação encapsulada.
  - `certificate` → preencha com o certificado codificado (no exemplo abaixo ele está vazio).
  - `Content-Type: application/json`.
3. Na aba **Body**, selecione `raw` + `JSON` e cole o payload completo apresentado abaixo (é o mesmo corpo usado no comando `curl`).
4. Clique em **Send**. Se as três aplicações estiverem rodando, a resposta retornará o payload anonimizado consumido da fila configurada.

#### Comando equivalente em cURL

```bash
curl --location 'http://localhost:8080/v1/anonymize/data' \
--header 'origin: Sistema' \
--header 'destination: Sistema' \
--header 'path: /open-insurance/consents/v2/consents' \
--header 'operation: POST' \
--header 'certificate;' \
--header 'Content-Type: application/json' \
--data-raw '{
  "data": {
    "consentId": "urn:initiator:C1DD93123",
    "expirationDateTime": "2021-05-21T08:30:00Z",
    "quoteCustomer": {
      "identificationData": {
        "updateDateTime": "2021-05-21T08:30:00Z",
        "personalId": "578-psd-71md6971kjh-2d414",
        "brandName": "Organização A",
        "civilName": "Juan Kaique Cláudio Fernandes",
        "socialName": "string",
        "cpfNumber": "84872401663",
        "companyInfo": {
          "cnpjNumber": "01773247000563",
          "name": "Empresa da Organização A"
        },
        "documents": [
          {
            "type": "CNH",
            "documentTypeOthers": "string",
            "number": "15291908",
            "expirationDate": "2023-05-21",
            "issueLocation": "string"
          }
        ],
        "hasBrazilianNationality": false,
        "otherNationalitiesInfo": "CAN",
        "otherDocuments": {
          "type": "SOCIAL SEC",
          "number": "15291908",
          "country": "string",
          "expirationDate": "2023-05-21"
        },
        "contact": {
          "postalAddresses": [
            {
              "address": "Av Naburo Ykesaki, 1270",
              "additionalInfo": "Fundos",
              "districtName": "Centro",
              "townName": "Marília",
              "countrySubDivision": "SP",
              "postCode": "10000000",
              "country": "BRA"
            }
          ],
          "phones": [
            {
              "countryCallingCode": "55",
              "areaCode": "19",
              "number": "29875132",
              "phoneExtension": "932"
            }
          ],
          "emails": [
            {
              "email": "nome@br.net"
            }
          ]
        },
        "civilStatusCode": "SOLTEIRO",
        "sex": "FEMININO",
        "birthDate": "2021-05-21",
        "filiation": {
          "type": "MAE",
          "civilName": "Marcelo Cláudio Fernandes"
        },
        "identificationDetails": {
          "civilName": "Juan Kaique Cláudio Fernandes",
          "cpfNumber": "63693941192"
        }
      },
      "qualificationData": {
        "updateDateTime": "2021-05-21T08:30:00Z",
        "pepIdentification": "NAO_EXPOSTO",
        "occupation": [
          {
            "details": "string",
            "occupationCode": "RECEITA_FEDERAL",
            "occupationCodeType": "RFB"
          }
        ],
        "lifePensionPlans": "SIM",
        "informedRevenue": {
          "incomeFrequency": "DIARIA",
          "currency": "BRL",
          "amount": "100000.04",
          "date": "2012-05-21"
        },
        "informedPatrimony": {
          "currency": "BRL",
          "amount": "100000.04",
          "year": "2010"
        }
      },
      "complimentaryInformationData": {
        "updateDateTime": "2021-05-21T08:30:00Z",
        "startDate": "2014-05-21",
        "relationshipBeginning": "2014-05-21",
        "productsServices": [
          {
            "contract": "string",
            "type": "MICROSSEGUROS",
            "insuranceLineCode": "6272",
            "procurators": [
              {
                "nature": "PROCURADOR",
                "cpfNumber": "73677831148",
                "civilName": "Elza Milena Stefany Teixeira",
                "socialName": "string"
              }
            ]
          }
        ]
      }
    },
    "historicalData": {
      "customer": {
        "identificationData": {
          "updateDateTime": "2021-05-21T08:30:00Z",
          "personalId": "578-psd-71md6971kjh-2d414",
          "brandName": "Organização A",
          "civilName": "Juan Kaique Cláudio Fernandes",
          "socialName": "string",
          "cpfNumber": "22220174155",
          "companyInfo": {
            "cnpjNumber": "01773247000563",
            "name": "Empresa da Organização A"
          },
          "documents": [
            {
              "type": "CNH",
              "documentTypeOthers": "string",
              "number": "15291908",
              "expirationDate": "2023-05-21",
              "issueLocation": "string"
            }
          ],
          "hasBrazilianNationality": false,
          "otherNationalitiesInfo": "CAN",
          "otherDocuments": {
            "type": "SOCIAL SEC",
            "number": "15291908",
            "country": "string",
            "expirationDate": "2023-05-21"
          },
          "contact": {
            "postalAddresses": [
              {
                "address": "Av Naburo Ykesaki, 1270",
                "additionalInfo": "Fundos",
                "districtName": "Centro",
                "townName": "Marília",
                "countrySubDivision": "SP",
                "postCode": "10000000",
                "country": "BRA"
              }
            ],
            "phones": [
              {
                "countryCallingCode": "55",
                "areaCode": "19",
                "number": "29875132",
                "phoneExtension": "932"
              }
            ],
            "emails": [
              {
                "email": "nome@br.net"
              }
            ]
          },
          "civilStatusCode": "SOLTEIRO",
          "sex": "FEMININO",
          "birthDate": "2021-05-21",
          "filiation": {
            "type": "MAE",
            "civilName": "Marcelo Cláudio Fernandes"
          },
          "identificationDetails": {
            "civilName": "Juan Kaique Cláudio Fernandes",
            "cpfNumber": "NA"
          }
        },
        "qualificationData": {
          "updateDateTime": "2021-05-21T08:30:00Z",
          "pepIdentification": "NAO_EXPOSTO",
          "occupation": [
            {
              "details": "string",
              "occupationCode": "RECEITA_FEDERAL",
              "occupationCodeType": "RFB"
            }
          ],
          "lifePensionPlans": "SIM",
          "informedRevenue": {
            "incomeFrequency": "DIARIA",
            "currency": "BRL",
            "amount": "100000.04",
            "date": "2012-05-21"
          },
          "informedPatrimony": {
            "currency": "BRL",
            "amount": "100000.04",
            "year": "2010"
          }
        },
        "complimentaryInformationData": {
          "updateDateTime": "2021-05-21T08:30:00Z",
          "startDate": "2014-05-21",
          "relationshipBeginning": "2014-05-21",
          "productsServices": [
            {
              "contract": "string",
              "type": "MICROSSEGUROS",
              "insuranceLineCode": "6272",
              "procurators": [
                {
                  "nature": "PROCURADOR",
                  "cpfNumber": "73677831148",
                  "civilName": "Elza Milena Stefany Teixeira",
                  "socialName": "string"
                }
              ]
            }
          ]
        }
      },
      "policies": [
        {
          "policyInfo": {
            "documentType": "APOLICE_INDIVIDUAL",
            "policyId": "111111",
            "susepProcessNumber": "string",
            "groupCertificateId": "string",
            "issuanceType": "EMISSAO_PROPRIA",
            "issuanceDate": "2022-12-31",
            "termStartDate": "2022-12-31",
            "termEndDate": "2022-12-31",
            "leadInsurerCode": "string",
            "leadInsurerPolicyId": "string",
            "maxLMG": {
              "amount": "95643871057.94",
              "unitType": "PORCENTAGEM",
              "unitTypeOthers": "Horas",
              "unit": {
                "code": "R$",
                "description": "BRL"
              }
            },
            "proposalId": "string",
            "insureds": [
              {
                "identification": "12345678900",
                "identificationType": "CPF",
                "identificationTypeOthers": "RNE",
                "name": "Nome Sobrenome",
                "birthDate": "1999-06-12",
                "postCode": "10000000",
                "email": "@pY&9*@|3~c|r6^bE,}4\\s/lBhj}|l0|O0v{)^M-aKiF_n+oxE>vo:_.2sqMZSPbCQ$T[tzq@O\\dKAGg",
                "city": "string",
                "state": "AC",
                "country": "BRA",
                "address": "string"
              }
            ],
            "beneficiaries": [
              {
                "identification": "12345678900",
                "identificationType": "CPF",
                "identificationTypeOthers": "RNE",
                "name": "Nome Sobrenome"
              }
            ],
            "principals": [
              {
                "identification": "12345678900",
                "identificationType": "CPF",
                "identificationTypeOthers": "RNE",
                "name": "Nome Sobrenome",
                "postCode": "10000000",
                "email": "^Sy],#vH8[BHZDGdU#@~_y]MB/{H%1/ZRLOQ*/9:N;97zYG#p35ZFRha]R!B0Mws%&<V'\''~`ozO)LhKa]=.H~\\.oN8;H@lWG`n#b;qs\".Q}N7^m&T{H)&+'\''-0p6+S@",
                "city": "string",
                "state": "AC",
                "country": "BRA",
                "address": "string",
                "addressAdditionalInfo": "Fundos"
              }
            ],
            "intermediaries": [
              {
                "type": "REPRESENTANTE",
                "typeOthers": "string",
                "identification": "12345678900",
                "brokerId": "456769825",
                "identificationType": "CPF",
                "identificationTypeOthers": "RNE",
                "name": "Nome Sobrenome",
                "postCode": "10000000",
                "city": "string",
                "state": "string",
                "country": "BRA",
                "address": "string"
              }
            ],
            "insuredObjects": [
              {
                "identification": "string",
                "type": "CONTRATO",
                "typeAdditionalInfo": "string",
                "description": "string",
                "amount": {
                  "amount": "100.\u0000",
                  "unitType": "PORCENTAGEM",
                  "unitTypeOthers": "Horas",
                  "unit": {
                    "code": "R$",
                    "description": "BRL"
                  }
                },
                "coverages": [
                  {
                    "branch": "0111",
                    "code": "DANOS_ELETRICOS",
                    "description": "string",
                    "internalCode": "string",
                    "susepProcessNumber": "string",
                    "LMI": {
                      "amount": "100.\u0000",
                      "unitType": "PORCENTAGEM",
                      "unitTypeOthers": "Horas",
                      "unit": {
                        "code": "R$",
                        "description": "BRL"
                      }
                    },
                    "isLMISublimit": true,
                    "termStartDate": "2022-12-31",
                    "termEndDate": "2022-12-31",
                    "isMainCoverage": true,
                    "feature": "MASSIFICADOS",
                    "type": "PARAMETRICO",
                    "gracePeriod": 0,
                    "gracePeriodicity": "DIA",
                    "gracePeriodCountingMethod": "DIAS_UTEIS",
                    "gracePeriodStartDate": "2022-12-31",
                    "gracePeriodEndDate": "2022-12-31",
                    "premiumPeriodicity": "MENSAL",
                    "premiumPeriodicityOthers": "string"
                  }
                ]
              }
            ],
            "coverages": [
              {
                "branch": "0111",
                "code": "DANOS_ELETRICOS",
                "description": "string",
                "deductible": {
                  "type": "DEDUTIVEL",
                  "typeAdditionalInfo": "string",
                  "amount": {
                    "amount": "081918",
                    "unitType": "PORCENTAGEM",
                    "unitTypeOthers": "Horas",
                    "unit": {
                      "code": "R$",
                      "description": "BRL"
                    }
                  },
                  "period": 10,
                  "periodicity": "DIA",
                  "periodCountingMethod": "DIAS_UTEIS",
                  "periodStartDate": "2022-05-16",
                  "periodEndDate": "2022-05-17",
                  "description": "Franquia de exemplo"
                },
                "POS": {
                  "applicationType": "VALOR",
                  "applicationTypeOthers": "string",
                  "description": "Descrição de exemplo",
                  "minValue": {
                    "amount": "00447",
                    "unitType": "PORCENTAGEM",
                    "unitTypeOthers": "Horas",
                    "unit": {
                      "code": "R$",
                      "description": "BRL"
                    }
                  },
                  "maxValue": {
                    "amount": "44",
                    "unitType": "PORCENTAGEM",
                    "unitTypeOthers": "Horas",
                    "unit": {
                      "code": "R$",
                      "description": "BRL"
                    }
                  },
                  "percentage": {
                    "amount": "023795325940.73",
                    "unitType": "PORCENTAGEM",
                    "unitTypeOthers": "Horas",
                    "unit": {
                      "code": "R$",
                      "description": "BRL"
                    }
                  },
                  "valueOthers": {
                    "amount": "9165",
                    "unitType": "PORCENTAGEM",
                    "unitTypeOthers": "Horas",
                    "unit": {
                      "code": "R$",
                      "description": "BRL"
                    }
                  }
                }
              }
            ],
            "coinsuranceRetainedPercentage": "10.00",
            "coinsurers": [
              {
                "identification": "string",
                "cededPercentage": "10.00"
              }
            ],
            "branchInfo": {
              "insuredObjects": [
                {
                  "identification": "string",
                  "propertyType": "CASA",
                  "propertyTypeAdditionalInfo": "string",
                  "postCode": "10000000",
                  "interestRate": "10.00",
                  "costRate": "10.00",
                  "updateIndex": "IPCA_IBGE",
                  "updateIndexOthers": "Índice de atualização",
                  "lenders": [
                    {
                      "companyName": "string",
                      "cnpjNumber": "12345678901234"
                    }
                  ]
                }
              ],
              "insureds": [
                {
                  "identification": "12345678900",
                  "identificationType": "CPF",
                  "identificationTypeOthers": "RNE",
                  "birthDate": "2022-12-31"
                }
              ]
            }
          },
          "premium": {
            "paymentsQuantity": 4,
            "amount": {
              "amount": "8.57",
              "unitType": "PORCENTAGEM",
              "unitTypeOthers": "Horas",
              "unit": {
                "code": "R$",
                "description": "BRL"
              }
            },
            "coverages": [
              {
                "branch": "0111",
                "code": "DANOS_ELETRICOS",
                "description": "string",
                "premiumAmount": {
                  "amount": "66.21",
                  "unitType": "PORCENTAGEM",
                  "unitTypeOthers": "Horas",
                  "unit": {
                    "code": "R$",
                    "description": "BRL"
                  }
                }
              }
            ],
            "payments": [
              {
                "movementDate": "2022-12-31",
                "movementType": "LIQUIDACAO_DE_PREMIO",
                "movementOrigin": "EMISSAO_DIRETA",
                "movementPaymentsNumber": "str",
                "amount": {
                  "amount": "99",
                  "unitType": "PORCENTAGEM",
                  "unitTypeOthers": "Horas",
                  "unit": {
                    "code": "R$",
                    "description": "BRL"
                  }
                },
                "maturityDate": "2022-12-31",
                "description": "Franquia de exemplo"
              }
            ]
          }
        }
      ]
    }
  }
}'
```

---

## 9. Referências Rápidas

- Priorize consistência de nomes entre ambientes.
- Padronize monitoração e alarmes para cada fila.
- Atualize este documento sempre que novas filas ou variáveis forem introduzidas.

---