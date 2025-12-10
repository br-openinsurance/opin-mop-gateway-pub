## Guia de Arquitetura e Configuração do Módulo de Operações OPIN (MOP) — MOP Client

Este documento consolidado descreve a estrutura de pacotes, a convenção de filas do RabbitMQ, os parâmetros de configuração Spring Boot e o processo de execução do **MOP Client**. Use-o como referência rápida para padronizar ambientes de desenvolvimento, homologação e produção.

## Sobre o MOP

O **Módulo de Operações OPIN (MOP)** foi desenvolvido para promover a transparência e confiabilidade no âmbito do Open Insurance.

**Principais objetivos:**
- Facilitar o acesso direto à infraestrutura dos participantes
- Eliminar obstáculos na comunicação entre sistemas
- Atuar como acelerador na implementação e análise de qualidade dos dados
- Prevenir riscos de cibersegurança e fraudes

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

## 3. Headers Obrigatórios da API

A API do Gateway requer os seguintes headers obrigatórios em todas as requisições:

| Header | Descrição | Valores Aceitos | Exemplo |
|--------|-----------|-----------------|---------|
| `origin` | Identifica o sistema originador da requisição | Qualquer string não vazia | `Sistema` |
| `destination` | Identifica o destino esperado pelo gateway | Qualquer string não vazia | `Sistema` |
| `path` | Rota alvo que será registrada nos logs | Qualquer string não vazia | `/open-insurance/consents/v2/consents` |
| `operation` | Método da operação encapsulada | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `CREATE`, `UPDATE`, `DELETE`, `PROCESS` | `POST` |
| `certificate` | Certificado codificado para autenticação/autorização | Qualquer string (pode ser vazia) | `cert-abc123xyz` |
| `userID` | Identificador do usuário ou sistema que está fazendo a requisição | Qualquer string não vazia | `user-12345` |
| `APPLICATION_MODE` | Modo de aplicação do gateway | **`TRANSMITTER`** ou **`RECEIVER`** (case-insensitive) | `TRANSMITTER` |

### 3.1 Validação do APPLICATION_MODE

O header `APPLICATION_MODE` é obrigatório e aceita apenas dois valores:
- **`TRANSMITTER`**: Usado quando o gateway está enviando mensagens para processamento
- **`RECEIVER`**: Usado quando o gateway está recebendo mensagens processadas

**Validações:**
- O header não pode estar vazio ou nulo
- Apenas os valores `TRANSMITTER` ou `RECEIVER` são aceitos (case-insensitive)
- Qualquer outro valor resultará em erro 400 com a mensagem: `"Header 'APPLICATION_MODE' must be either 'TRANSMITTER' or 'RECEIVER'"`

### 3.2 Exemplo de Requisição com APPLICATION_MODE

```bash
curl --location 'http://localhost:8080/v1/anonymize/data' \
--header 'origin: Sistema' \
--header 'destination: Sistema' \
--header 'path: /open-insurance/consents/v2/consents' \
--header 'operation: POST' \
--header 'certificate: cert-abc123' \
--header 'userID: user-12345' \
--header 'APPLICATION_MODE: TRANSMITTER' \
--header 'Content-Type: application/json' \
--data-raw '{"data": {...}}'
```

---

## 4. Mensageria RabbitMQ

### 4.1 Filas Mandatórias

Estas filas devem existir em todos os ambientes:

| Fila | Descrição | Produtor | Consumidor |
|------|-----------|----------|------------|
| `data.anonymization.input.queue` | Recebe dados brutos para anonimização | `open-insurance-mop-gateway` | [`open-insurance-mop-client-anonymization`](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub/tree/develop) |
| `data.anonymization.output.queue` | Armazena dados já anonimizados para processamento ou armazenamento | [`open-insurance-mop-client-anonymization`](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub/tree/develop) | [`open-insurance-mop-validator`](https://github.com/br-openinsurance/mop-client-data-validator-pub/tree/develop) |
| `data.validator.input.queue` | Fila intermediária para payloads recebidos pelo gateway que são validados | [`open-insurance-mop-validator`](https://github.com/br-openinsurance/mop-client-data-validator-pub/tree/develop) | `open-insurance-mop-gateway` |

### 4.2 Diretrizes de Configuração

- **Durável** (`durable=true`) para sobreviver a reinícios.
- **Auto-delete** desativado.
- **Dead Letter Queue** configurada quando aplicável (ex.: `data.anonymization.input.dlq`).
- **Formato de mensagem** em JSON com campos mínimos `id`, `timestamp`, `payload`, `requestId`.
- **Segurança**: TLS, autenticação e autorização habilitadas.
- **Monitoramento**: acompanhar throughput, latência e falhas por fila.
- **Nomenclatura**: letras minúsculas e pontos (`.`) como separadores; evite nomes genéricos.

---

## 4. Configuração Spring Boot

### 5.1 Aplicação
```yaml
spring.application.name: ${SPRING_APPLICATION_NAME:mop-client-gateway}
```
O valor à direita do `:` indica o padrão utilizado quando `SPRING_APPLICATION_NAME` não é informado; se nenhum valor for definido, a aplicação sobe como `mop-client-gateway`.

### 5.2 Servidor
```yaml
server.port: ${SERVER_PORT:8080}
```
Assim como nas demais propriedades, o valor padrão é aplicado quando `SERVER_PORT` não é definido externamente.

### 5.3 RabbitMQ
```yaml
# Unified Spring AMQP Configuration
spring.rabbitmq:
  # Connection settings
  host: ${RABBITMQ_VALIDATOR_HOST:localhost}
  port: ${RABBITMQ_VALIDATOR_PORT:5672}
  username: ${RABBITMQ_USERNAME:guest}
  password: ${RABBITMQ_PASSWORD:guest}
  
  # Listener configuration
  listener:
    simple:
      acknowledge-mode: auto
      concurrency: 1
      max-concurrency: 5
      prefetch: 10
  
  # Application-specific queues
  queues:
    validator:
      name: ${RABBITMQ_VALIDATOR_QUEUE_NAME:data.validator.input.queue}
    output:
      name: ${RABBITMQ_OUTPUT_QUEUE_NAME:data.anonymization.output.queue}
  
  # Retry configuration
  retry:
    maxAttempts: ${RABBITMQ_RETRY_MAX_ATTEMPTS:5}
    backoff: ${RABBITMQ_RETRY_BACKOFF:2000}
    enablesTransactionSupport: ${RABBITMQ_ENABLES_TRANSACTION_SUPPORT:true}
```
Todos os parâmetros podem ser sobrescritos por variáveis de ambiente com o mesmo nome (`RABBITMQ_*`).

### 5.4 Endpoints de Gerenciamento
```yaml
management.endpoints.web.exposure.include: '*'
management.endpoint.health.show-details: always
```
Expõe todos os endpoints do Actuator e força detalhes completos no `/actuator/health`.

---

## 6. Variáveis de Ambiente

As tabelas abaixo mapeiam as propriedades Spring expostas por cada aplicação para suas respectivas variáveis de ambiente e valores padrão.

### 6.1 Gateway (`open-insurance-mop-gateway`)

| Variável | Propriedade Spring | Valor padrão |
|----------|--------------------|--------------|
| `SERVER_PORT_GATEWAY` | `server.port` | `8080` |
| `RABBITMQ_USERNAME` | `spring.rabbitmq.username` | `guest` |
| `RABBITMQ_PASSWORD` | `spring.rabbitmq.password` | `guest` |
| `RABBITMQ_VALIDATOR_HOST` | `spring.rabbitmq.host` | `localhost` |
| `RABBITMQ_VALIDATOR_PORT` | `spring.rabbitmq.port` | `5672` |
| `RABBITMQ_RETRY_MAX_ATTEMPTS` | `spring.rabbitmq.retry.maxAttempts` | `5` |
| `RABBITMQ_RETRY_BACKOFF` | `spring.rabbitmq.retry.backoff` | `2000` |
| `RABBITMQ_ENABLES_TRANSACTION_SUPPORT` | `spring.rabbitmq.retry.enablesTransactionSupport` | `true` |
| `RABBITMQ_VALIDATOR_QUEUE_NAME` | `spring.rabbitmq.queues.validator.name` | `data.validator.input.queue` |
| `RABBITMQ_OUTPUT_QUEUE_NAME` | `spring.rabbitmq.queues.output.name` | `data.anonymization.output.queue` |

### 6.2 Validator ([`open-insurance-mop-validator`](https://github.com/br-openinsurance/mop-client-data-validator-pub/tree/develop))

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


### 6.3 Anonymization ([`open-insurance-mop-anonymization`](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub/tree/develop))

| Variável | Propriedade Spring | Valor padrão |
|----------|--------------------|--------------|
| `SERVER_PORT_ANONYMIZATION` | `server.port` | `8181` |
| `EXTERNAL_API_DATA_ANONYMIZATION` | `external.api.data-anonymization` | `http://mop-server-entrypoint-dev.intranet.opinbrasil/anonymization-fields?schema=Consent` |
| `EXTERNAL_REQUEST_URL` | `external.request.url` | `http://mop-server-entrypoint-dev.intranet.opinbrasil/process` |
| `EXTERNAL_REQUEST_HOST` | `external.request.host` | `mop-server-entrypoint-dev.intranet.opinbrasil` |
| `EXTERNAL_REQUEST_PATH` | `external.request.path` | `/process` |
| `EXTERNAL_REQUEST_METHOD` | `external.request.method` | `POST` |
| `RABBITMQ_USERNAME` | `rabbitmq.username` | `guest` |
| `RABBITMQ_PASSWORD` | `rabbitmq.password` | `guest` |
| `RABBITMQ_INPUT_QUEUE_NAME` | `rabbitmq.input.queue.name` | `data.anonymization.input.queue` |
| `RABBITMQ_INPUT_HOST` | `rabbitmq.input.host` | `localhost` |
| `RABBITMQ_INPUT_PORT` | `rabbitmq.input.port` | `5672` |
| `RABBITMQ_OUTPUT_QUEUE_NAME` | `rabbitmq.output.queue.name` | `data.anonymization.output.queue` |
| `RABBITMQ_OUTPUT_HOST` | `rabbitmq.output.host` | `localhost` |
| `RABBITMQ_OUTPUT_PORT` | `rabbitmq.output.port` | `5672` |
---

## 7. Execução Local

### 7.1 Pré-requisitos

- Java 17
- Maven 3.x
- Docker
- Docker Compose

### 7.2 Configuração do RabbitMQ via Docker Compose

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

## 8. Passos via Docker

1. Baixe as imagens obrigatórias (gateway, validator e anonymization):
   ```bash
   docker pull ghcr.io/br-openinsurance/opin-mop-gateway-pub/open-insurance-mop-gateway:develop
   docker pull ghcr.io/br-openinsurance/mop-client-data-validator-pub/open-insurance-mop-validator:develop
   docker pull ghcr.io/br-openinsurance/opin-mop-client-anonymization-pub/open-insurance-mop-anonymization-pub:develop
   ```
2. Execute os containers exportando as variáveis necessárias (exemplo POSIX):

   **Gateway**
   ```bash
   docker run --rm --name mop-gateway \
     -e SERVER_PORT=${SERVER_PORT_GATEWAY:-8080} \
     -e RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest} \
     -e RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest} \
     -e RABBITMQ_RETRY_MAX_ATTEMPTS=${RABBITMQ_RETRY_MAX_ATTEMPTS:-5} \
     -e RABBITMQ_RETRY_BACKOFF=${RABBITMQ_RETRY_BACKOFF:-2000} \
     -e RABBITMQ_ENABLES_TRANSACTION_SUPPORT=${RABBITMQ_ENABLES_TRANSACTION_SUPPORT:-true} \
     -e RABBITMQ_VALIDATOR_QUEUE_NAME=${RABBITMQ_VALIDATOR_QUEUE_NAME:-data.anonymization.input.queue} \
     -e RABBITMQ_VALIDATOR_HOST=${RABBITMQ_VALIDATOR_HOST:-localhost} \
     -e RABBITMQ_VALIDATOR_PORT=${RABBITMQ_VALIDATOR_PORT:-5672} \
     -p ${SERVER_PORT_GATEWAY:-8080}:${SERVER_PORT_GATEWAY:-8080} \
     ghcr.io/br-openinsurance/opin-mop-gateway-pub/open-insurance-mop-gateway:develop
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
     ghcr.io/br-openinsurance/mop-client-data-validator-pub/open-insurance-mop-validator:develop
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

### 8.1 Comportamento dos valores padrão no Docker

- A sintaxe `${VARIAVEL:-valorPadrao}` é avaliada pelo shell antes do `docker run`. Se `VARIAVEL` estiver definida no seu terminal, o valor informado é usado; caso contrário, o texto após os dois pontos é enviado para o container.
- Isso significa que você pode omitir qualquer `-e` e confiar no valor padrão mostrado no comando, obtendo o mesmo comportamento descrito na tabela de variáveis de ambiente.
- Para personalizar um valor, defina a variável no shell (ex.: `export SERVER_PORT=9090`) antes de executar o comando ou substitua diretamente o trecho `${VARIAVEL:-valorPadrao}` pelo valor desejado.

## 9. Consulta ao diagrama de arquitetura do cliente

1. Localize o arquivo `docs/assets/mop-client-arquitetura.png`, que contém o diagrama oficial do MOP Client.
2. Abra a imagem para revisar o fluxo ponta a ponta (produtor → broker → consumidor) antes de configurar novas integrações.
3. Caso o arquivo ainda não esteja presente, utilize a imagem compartilhada pela equipe de arquitetura e salve-a com o mesmo nome para manter o documento sincronizado.

### 9.1 Exemplo de requisição via Postman/cURL

Use este passo a passo para validar rapidamente o endpoint `/v1/anonymize/data` depois de subir os containers:

1. Abra o Postman e crie uma requisição `POST` apontando para `http://localhost:8080/v1/anonymize/data`.
2. Na aba **Headers**, inclua:
  - `origin: Sistema` → identifica o sistema originador.
  - `destination: Sistema` → identifica o destino esperado pelo gateway.
  - `path: /open-insurance/consents/v2/consents` → rota alvo que será registrada nos logs.
  - `operation: POST` → método da operação encapsulada.
  - `certificate` → preencha com o certificado codificado (no exemplo abaixo ele está vazio).
  - `userID: user-12345` → identificador do usuário ou sistema que está fazendo a requisição.
  - `APPLICATION_MODE: TRANSMITTER` → modo de aplicação: `TRANSMITTER` (para envio de mensagens) ou `RECEIVER` (para recebimento de mensagens). **Obrigatório**.
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
--header 'certificate:' \
--header 'userID: user-12345' \
--header 'APPLICATION_MODE: TRANSMITTER' \
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
        "civilName": "Fulano de Tal",
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
              "address": "Rua Exemplo, 123",
              "additionalInfo": "Apto 101",
              "districtName": "Centro",
              "townName": "São Paulo",
              "countrySubDivision": "SP",
              "postCode": "10000000",
              "country": "BRA"
            }
          ],
          "phones": [
            {
              "countryCallingCode": "55",
              "areaCode": "19",
              "number": "12345678",
              "phoneExtension": "932"
            }
          ],
          "emails": [
            {
              "email": "exemplo@exemplo.com.br"
            }
          ]
        },
        "civilStatusCode": "SOLTEIRO",
        "sex": "FEMININO",
        "birthDate": "2021-05-21",
        "filiation": {
          "type": "MAE",
          "civilName": "Maria da Silva"
        },
        "identificationDetails": {
          "civilName": "Fulano de Tal",
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
                "civilName": "José da Silva",
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
          "civilName": "Fulano de Tal",
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
                "number": "12345678",
                "phoneExtension": "932"
              }
            ],
            "emails": [
              {
                "email": "exemplo@exemplo.com.br"
              }
            ]
          },
          "civilStatusCode": "SOLTEIRO",
          "sex": "FEMININO",
          "birthDate": "2021-05-21",
          "filiation": {
            "type": "MAE",
            "civilName": "Maria da Silva"
          },
          "identificationDetails": {
            "civilName": "Fulano de Tal",
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
                  "civilName": "José da Silva",
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

## 10. Referências Rápidas

- Priorize consistência de nomes entre ambientes.
- Padronize monitoração e alarmes para cada fila.
- Atualize este documento sempre que novas filas ou variáveis forem introduzidas.

---
