# Arquitetura

Este documento descreve a arquitetura do MOP Client, incluindo diagramas, camadas e fluxo de mensagens.

## Visão Geral

O MOP Client segue uma **arquitetura em camadas** com separação clara de responsabilidades, facilitando manutenção, testes e evolução do sistema.

## Camadas da Aplicação

```
┌─────────────────────────────────────────────┐
│  Interface Layer (Controllers/Validation)   │
│  - AnonymizerController                     │
│  - HeaderValidator                          │
├─────────────────────────────────────────────┤
│  Application Layer (Services)              │
│  - RabbitMQMessageService                   │
│  - JsonPayloadParser                        │
│  - RequestHeadersBuilder                    │
│  - TraceabilityService                      │
│  - ResponseBuilder                          │
├─────────────────────────────────────────────┤
│  Infrastructure Layer (Adapters)            │
│  - RabbitMQMessagePublisher                │
│  - RabbitListener                           │
├─────────────────────────────────────────────┤
│  Shared Layer (Utils/Exceptions)            │
│  - MessageBuilderHelper                     │
│  - CorrelationIdContext                     │
│  - JsonUtil                                 │
└─────────────────────────────────────────────┘
```

### Interface Layer (Camada de Interface)

Responsável pela exposição da API e validação de entrada:

- **AnonymizerController**: Controlador REST que recebe requisições HTTP
- **HeaderValidator**: Validação de headers obrigatórios e regras de negócio

### Application Layer (Camada de Aplicação)

Contém a lógica de negócio e orquestração:

- **RabbitMQMessageService**: Serviço principal que orquestra o envio de mensagens
- **JsonPayloadParser**: Parser e normalização de payloads JSON
- **RequestHeadersBuilder**: Construção de DTOs com informações de traceability
- **TraceabilityService**: Geração de correlationID e timestamp
- **ResponseBuilder**: Construção de respostas HTTP estruturadas

### Infrastructure Layer (Camada de Infraestrutura)

Adaptadores para tecnologias externas:

- **RabbitMQMessagePublisher**: Publicação de mensagens no RabbitMQ
- **RabbitListener**: Consumo de mensagens do RabbitMQ

### Shared Layer (Camada Compartilhada)

Utilitários e exceções compartilhadas:

- **MessageBuilderHelper**: Helpers para construção de mensagens
- **CorrelationIdContext**: Contexto para rastreamento de correlationID
- **JsonUtil**: Utilitários para manipulação de JSON

## Fluxo de Mensagens

### Fluxo Completo no Ecossistema MOP

```
1. Cliente HTTP
   │
   │ POST /v1/anonymize/data
   │ Headers: origin, destination, path, operation, userID, applicationMode
   │ Body: (opcional) JSON payload
   │
   ▼
2. Gateway (MOP Client)
   │
   │ ├─ HeaderValidator → Valida headers obrigatórios
   │ ├─ JsonPayloadParser → Processa payload (aceita vazio/null)
   │ ├─ RequestHeadersBuilder → Constrói DTO com traceability
   │ │  └─ TraceabilityService → Gera correlationID e timestamp
   │ └─ RabbitMQMessageService → Envia mensagem
   │
   ▼
3. RabbitMQ
   │
   │ Fila: data.anonymization.input.queue
   │ Fila que recebe os dados da requisição HTTP de entrada
   │
   ▼
4. Anonymization Service
   │
   │ ├─ Consome da fila de input
   │ ├─ Processa anonimização (remove dados sensíveis)
   │ └─ Publica na fila de output
   │
   ▼
5. RabbitMQ
   │
   │ Fila: data.anonymization.output.queue
   │ Fila que contém os dados anonimizados de acordo com a 
   │ configuração de cada participante, removendo dados sensíveis 
   │ e mantendo apenas a estrutura sem dados
   │
   ▼
6. Validator Service
   │
   │ ├─ Consome da fila de output
   │ ├─ Valida dados (informa quais arquivos precisam de validação)
   │ └─ Publica na fila de validação
   │
   ▼
7. RabbitMQ
   │
   │ Fila: data.validator.input.queue
   │ Fila que armazena os dados validados pelo validador,
   │ informando quais arquivos precisam de validação
   │
   ▼
8. Gateway (MOP Client)
   │
   │ RabbitListener → Consome mensagem validada
   │
   ▼
9. Processamento Final
```

### Filas do Ecossistema MOP

O ecossistema utiliza três filas principais para comunicação assíncrona:

#### `data.anonymization.input.queue`

**Função:** Fila que recebe os dados da requisição HTTP de entrada

**Características:**
- **Produtor:** Gateway (MOP Client)
- **Consumidor:** Anonymization Service
- **Conteúdo:** Dados brutos recebidos via requisição HTTP, incluindo headers de traceability (correlationID, timestamp, origin, destination, path, operation, applicationMode)
- **Formato:** JSON com payload original e metadados

#### `data.anonymization.output.queue`

**Função:** Fila que contém os dados anonimizados de acordo com a configuração de cada participante

**Características:**
- **Produtor:** Anonymization Service
- **Consumidor:** Validator Service
- **Conteúdo:** Dados anonimizados onde dados sensíveis foram removidos, mantendo apenas a estrutura sem dados
- **Processamento:** A anonimização é realizada conforme configuração específica de cada participante, removendo informações sensíveis mas preservando a estrutura do documento

#### `data.validator.input.queue`

**Função:** Fila que armazena os dados validados pelo validador

**Características:**
- **Produtor:** Validator Service
- **Consumidor:** Gateway (MOP Client)
- **Conteúdo:** Dados validados com informações sobre quais arquivos precisam de validação
- **Informações:** O validador identifica e informa quais arquivos/documentos necessitam de validação adicional

### Fluxo Interno do Gateway

```
Requisição HTTP
    │
    ▼
┌─────────────────────────────────┐
│ AnonymizerController            │
│ - Recebe requisição HTTP        │
│ - Extrai headers e body         │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ HeaderValidator                 │
│ - Valida headers obrigatórios   │
│ - Valida applicationMode        │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ JsonPayloadParser               │
│ - Parse ou normaliza payload    │
│ - Trata vazio/null como {}      │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ RequestHeadersBuilder           │
│ - Constrói RequestHeadersDTO    │
│ - Inclui traceability           │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ TraceabilityService             │
│ - Gera correlationID            │
│ - Gera timestamp                │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ RabbitMQMessageService          │
│ - Orquestra envio               │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ RabbitMQMessagePublisher        │
│ - Publica no RabbitMQ           │
└────────────┬────────────────────┘
             │
             ▼
         RabbitMQ
```

## Princípios de Design

### Separação de Responsabilidades

Cada camada tem responsabilidades bem definidas:

- **Interface**: Validação e exposição
- **Application**: Lógica de negócio
- **Infrastructure**: Integração com tecnologias externas
- **Shared**: Utilitários compartilhados

### Dependency Injection

A aplicação utiliza injeção de dependências para facilitar testes e manutenção.

### Tratamento de Erros

- Validações em múltiplas camadas
- Exceções customizadas (`BusinessException`, `ErrorResponseException`, `RabbitMQException`)
- Respostas HTTP estruturadas com detalhes de erro

### Traceability

- CorrelationID gerado automaticamente
- Timestamp ISO-8601
- Rastreamento através de todas as camadas

## Diagrama de Arquitetura

Para visualização completa, consulte o diagrama em:

![Diagrama de Arquitetura](assets/mop-client-arquitetura.png)

## Referências

- [Spring Boot Architecture](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

