# MOP Client Gateway - Wiki Técnico

Bem-vindo ao wiki técnico do **MOP Client Gateway**. Este documento fornece informações detalhadas sobre arquitetura, implementação, configuração avançada e boas práticas do projeto.

## 📑 Índice

1. [Visão Geral e Contexto](#visão-geral-e-contexto)
2. [Arquitetura do Sistema](#arquitetura-do-sistema)
3. [Componentes e Responsabilidades](#componentes-e-responsabilidades)
4. [Fluxo de Dados Detalhado](#fluxo-de-dados-detalhado)
5. [Configuração Avançada](#configuração-avançada)
6. [RabbitMQ - Configuração e Uso](#rabbitmq---configuração-e-uso)
7. [Rastreamento e Correlation IDs](#rastreamento-e-correlation-ids)
8. [Tratamento de Erros](#tratamento-de-erros)
9. [Performance e Otimizações](#performance-e-otimizações)
10. [Segurança](#segurança)
11. [Monitoramento e Observabilidade](#monitoramento-e-observabilidade)
12. [Desenvolvimento e Contribuição](#desenvolvimento-e-contribuição)
13. [Troubleshooting Avançado](#troubleshooting-avançado)
14. [Referências e Documentação Adicional](#referências-e-documentação-adicional)

---

## Visão Geral e Contexto

### O que é o MOP Client Gateway?

O **MOP Client Gateway** é um serviço de gateway que faz parte do ecossistema **Módulo de Operações OPIN (MOP)** no contexto do **Open Insurance Brasil**. Ele atua como ponto de entrada para requisições de anonimização de dados, garantindo:

- **Validação** de requisições HTTP
- **Rastreamento** completo através de correlation IDs
- **Orquestração** de mensagens via RabbitMQ
- **Integração** com serviços externos de processamento

### Módulo de Operações OPIN (MOP) - Contexto e Objetivos

O **Módulo de Operações OPIN (MOP)** foi desenvolvido para promover a **transparência e confiabilidade** no âmbito do Open Insurance Brasil. A plataforma visa:

#### Objetivos Principais

- **Garantia da precisão de dados**: Através da verificação de integridade e validação do formato e conformidade dos dados
- **Monitoramento em tempo real**: Para detecção de anomalias e alertas proativos
- **Conformidade regulatória**: Auxiliando na adequação à regulação do OPIN e aderência às regras de privacidade e segurança
- **Redução de custos e ineficiências**: Facilitando a implementação, reduzindo a necessidade de ajustes e intervenções
- **Fornecimento de dados de alta qualidade**: Que possam ser usados com confiança para análise e tomada de decisões estratégicas no âmbito regulador e de mercado

#### Motivação

A motivação para desenvolvimento do MOP é:
- Facilitar o acesso direto à infraestrutura dos participantes
- Eliminar obstáculos na comunicação entre sistemas
- Atuar como acelerador na implementação e análise de qualidade dos dados
- Prevenir riscos de cibersegurança e fraudes

### Evolução do Processo OPIN

A evolução do processo OPIN pode ser visualizada em três estágios distintos, conforme ilustrado no diagrama oficial:

#### 1. Processo Atual

No modelo atual, a comunicação é direta entre os participantes:

```
┌─────┐                    ┌─────┐
│ RP  │                    │ OP  │
│(RP) │                    │(OP) │
└──┬──┘                    └──┬──┘
   │                          │
   │      Request             │
   ├─────────────────────────>│
   │                          │
   │      Response            │
   │<─────────────────────────┤
   │                          │
   ▼                          ▼
┌─────────────────────────────┐
│          PCM                │
│   (Ponto Central de          │
│    Mensageria)               │
└─────────────────────────────┘
```

**Características**:
- Comunicação direta entre **RP (Relying Party)** e **OP (Open Provider)**
- Intermediação através do **PCM (Ponto Central de Mensageria)**
- Fluxo síncrono Request/Response
- Sem processamento intermediário de dados

**Nota**: Este é o modelo atualmente em produção no Open Finance BR.

#### 2. Evolução¹ (Modelo Container MOP)

Na primeira evolução, são introduzidos os Containers MOP:

```
┌─────┐                    ┌─────┐
│ RP  │                    │ OP  │
└──┬──┘                    └──┬──┘
   │                          │
   │      Request             │
   ├─────────────────────────>│
   │                          │
   │      Response            │
   │<─────────────────────────┤
   │                          │
   ▼                          ▼
┌─────────────────┐  ┌─────────────────┐
│ Container MOP   │  │ Container MOP   │
│  (no RP)        │  │  (no OP)        │
└────────┬────────┘  └────────┬────────┘
         │                    │
         └──────────┬─────────┘
                    │
                    ▼
         ┌──────────────────┐
         │   Estrutura OPIN  │
         │  (Centralizada)   │
         └──────────────────┘
```

**Características**:
- **Container MOP** instalado em cada participante (RP e OP)
- Containers se conectam à **Estrutura OPIN** centralizada
- Processamento assíncrono de dados
- Validação e anonimização intermediária
- Comunicação Request/Response ainda direta entre RP e OP

**Módulos Iniciais do Container MOP**:
- **Dados**: Validação e armazenamento anonimizado
- **Qualidade**: Testes de validação estrutural das APIs
- **Segurança**: Centralização de logs e eventos de segurança

**Nota**: Este é o modelo proposto em discussão pelo regulador Open Finance, após recomendações do Open Insurance.

#### 3. Visão Final² (Modelo Evolutivo Completo)

Na visão final, os Containers MOP se comunicam entre si:

```
┌─────┐                    ┌─────┐
│ RP  │                    │ OP  │
└──┬──┘                    └──┬──┘
   │                          │
   ▼                          ▼
┌─────────────────┐  ┌─────────────────┐
│ Container MOP   │  │ Container MOP   │
│  (Transmissão)  │  │  (Recepção)     │
└────────┬────────┘  └────────┬────────┘
         │                    │
         │    Request         │
         ├───────────────────>│
         │                    │
         │    Response        │
         │<───────────────────┤
         │                    │
         └──────────┬─────────┘
                    │
                    ▼
         ┌──────────────────┐
         │   Estrutura OPIN  │
         │  (Monitoramento,  │
         │   Reports, Dados) │
         └──────────────────┘
```

**Características**:
- **Comunicação entre Containers MOP**: Request/Response agora fluem entre os Containers MOP
- **Processamento completo**: Validação, anonimização e qualidade antes da comunicação
- **Rastreamento completo**: Todas as transações são rastreadas e reportadas
- **Módulos completos**: Todos os módulos (Transmissão, Recepção, Dados, Qualidade, Segurança, Fraudes, Certificados, Integração, Jornadas)

**Fluxo de Comunicação**:
1. RP envia Request → Container MOP (Transmissão) do RP
2. Container MOP (Transmissão) processa e valida
3. Request é enviado → Container MOP (Recepção) do OP
4. Container MOP (Recepção) processa e valida
5. Request chega ao OP
6. OP processa e envia Response → Container MOP (Recepção) do OP
7. Response é processado e enviado → Container MOP (Transmissão) do RP
8. Response chega ao RP
9. Ambos os Containers reportam à Estrutura OPIN

**Benefícios da Visão Final**:
- **Controle transacional em real time**: Monitoramento completo do fluxo
- **Maior robustez**: Validações em múltiplos pontos
- **Geração de insights**: Dados estatísticos do ecossistema
- **Prevenção contra fraudes**: Análise comportamental
- **Maior conformidade**: Certificação e controle de certificados

### Modelo Container IaC

A plataforma, em modelo **container IaC (Infrastructure as Code)**, é disponibilizada em **código aberto** para as instituições e hospedada nas respectivas infraestruturas.

**Características do Container**:
- **Código aberto**: Disponível publicamente
- **IaC**: Infraestrutura como código
- **Hospedagem distribuída**: Cada participante hospeda seu próprio container
- **Modular**: Módulos independentes e acopláveis

### Contexto do Open Insurance

O Open Insurance é uma iniciativa regulatória que visa promover transparência e confiabilidade no mercado de seguros brasileiro. O MOP Client Gateway facilita a comunicação entre sistemas participantes, garantindo:

- Qualidade dos dados
- Rastreabilidade completa
- Processamento assíncrono
- Anonimização de dados sensíveis

### Ecossistema MOP

O gateway trabalha em conjunto com outros serviços do ecossistema MOP:

```
┌─────────────────┐
│   Gateway       │ ← Recebe requisições HTTP
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   RabbitMQ      │ ← Fila de mensagens
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌─────────┐ ┌──────────┐
│Anonymize│ │ Validator│
└─────────┘ └──────────┘
```

### Módulos do MOP

O ecossistema MOP é composto por múltiplos módulos que trabalham em conjunto:

#### Primeira Entrega (Atual)

1. **Módulo de Dados**
   - Validação e armazenamento de cópia anonimizada dos dados transacionados
   - Reports de métricas regulatórios
   - Metadados para análises estatísticas do ecossistema
   - **Dores atacadas**: Inconsistência nos reports de dados, maior amplitude de visão das métricas

2. **Módulo de Qualidade**
   - Testes de validação estrutural das APIs vigentes
   - Execução de testes através do Motor
   - **Dores atacadas**: Automação da certificação estrutural e validação em produção das APIs

3. **Módulo de Segurança da Informação**
   - Centralizador de logs e eventos relacionados à segurança
   - Detecção de riscos pelos participantes do ecossistema
   - Plataforma MISP
   - **Dores atacadas**: Maior visibilidade e controle dos itens e riscos de segurança

#### Visão Final (Evolução)

4. **Módulo de Transmissão e Recepção**
   - Execução dos comportamentos de transmissão e recepção conforme especificações do Open Insurance
   - **Dores atacadas**: Otimização da implementação transacional, garantia do cumprimento regulatório

5. **Módulo de Fraudes**
   - Centralização de registros para análise de comportamento
   - Análise de chamadas OPIN e comportamento de clientes e participantes
   - Implementado em ledger com LLM para análise in loco
   - **Dores atacadas**: Monitoramento em tempo real contra ataques e fraudes, registro permanente das transações

6. **Módulo de Integração**
   - Validação da conectividade das APIs
   - Comportamentos relacionados à integração
   - **Dores atacadas**: Garantia de disponibilidade e nível de serviço das APIs

7. **Módulo de Certificados**
   - Execução de testes de certificação (FAPI OP / RP)
   - Validação da operação dos certificados ICP-Brasil
   - **Dores atacadas**: Falta de visibilidade de mudanças de infraestrutura, garantia de compliance e segurança

8. **Módulo de Jornadas**
   - Leitura e validação das implementações das participantes
   - Baseado no guia de experiência do usuário
   - **Dores atacadas**: Automação do controle de conformidade das participantes do ponto de vista de UX

### Arquitetura do Ecossistema MOP

#### Visão Final - Arquitetura Proposta

```
┌─────────────────────────────────────────────────────────────┐
│                    Perímetro Central                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Node OPIN    │  │ Node Admin   │  │ Report       │     │
│  │ Monitoramento│  │              │  │ Service      │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Ledger dados não anonimizados                       │  │
│  │  Delta Lake dados anonimizados                       │  │
│  │  Dados Estatísticos                                  │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          ▲
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
┌───────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐
│ Participante A│  │ Participante B│  │ Participante C│
│               │  │               │  │               │
│ Container MOP│  │ Container MOP │  │ Container MOP │
│  - Dados     │  │  - Dados     │  │  - Dados     │
│  - Qualidade │  │  - Qualidade │  │  - Qualidade │
│  - Segurança │  │  - Segurança │  │  - Segurança │
│  - Transmissão│ │  - Transmissão│ │  - Transmissão│
│  - Recepção  │  │  - Recepção  │  │  - Recepção  │
└──────────────┘  └──────────────┘  └──────────────┘
```

### Benefícios Esperados

#### Primeira Entrega
- Redução nos erros e inconsistências nos reports de métricas do ecossistema
- Automação e melhoria no controle à ameaças de segurança
- Redução esforço operacional para garantia de conformidade das APIs

#### Modelo Final
- Controle transacional em real time
- Maior robustez na execução das transações
- Geração de insights estatísticos para o ecossistema
- Prevenção contra fraudes
- Maior conformidade no processo de certificação e controle de certificados

---

## Arquitetura do Sistema

### Arquitetura em Camadas

O projeto segue uma **arquitetura em camadas** bem definida, separando responsabilidades:

```
┌─────────────────────────────────────────────────────────┐
│              Interface Layer (Controllers)               │
│  - AnonymizerController                                  │
│  - HeaderValidator                                       │
│  - CorrelationIdFilter                                  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│           Application Layer (Services)                  │
│  - RabbitMQMessageService                               │
│  - JsonPayloadParser                                    │
│  - RequestHeadersBuilder                                │
│  - ResponseBuilder                                      │
│  - TraceabilityService                                  │
│  - ExternalApiClient                                    │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│        Infrastructure Layer (Adapters)                  │
│  - RabbitMQMessagePublisher                             │
│  - RabbitListener                                       │
│  - RabbitMQConfig                                       │
│  - RestTemplateConfig                                   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│            Shared Layer (Utils/Exceptions)               │
│  - MessageBuilderHelper                                 │
│  - CorrelationIdContext                                │
│  - JsonUtil                                            │
│  - GlobalExceptionHandler                               │
└─────────────────────────────────────────────────────────┘
```

### Princípios de Design

1. **Separation of Concerns**: Cada camada tem responsabilidades bem definidas
2. **Dependency Inversion**: Camadas superiores dependem de abstrações
3. **Single Responsibility**: Cada classe tem uma única responsabilidade
4. **Open/Closed Principle**: Aberto para extensão, fechado para modificação

### Padrões Utilizados

- **Builder Pattern**: Para construção de DTOs complexos
- **Strategy Pattern**: Para diferentes tipos de processamento
- **Template Method**: Para fluxos de processamento padronizados
- **Observer Pattern**: Para eventos de inicialização

---

## Componentes e Responsabilidades

### Interface Layer

#### AnonymizerController

**Responsabilidade**: Receber e processar requisições HTTP de anonimização.

**Funcionalidades**:
- Validação de headers obrigatórios
- Parsing de payload JSON (aceita vazio/null)
- Construção de DTOs de requisição
- Orquestração do fluxo de processamento
- Construção de respostas padronizadas

**Fluxo**:
1. Recebe requisição POST
2. Valida headers via `HeaderValidator`
3. Parse do JSON via `JsonPayloadParser`
4. Constrói headers DTO via `RequestHeadersBuilder`
5. Envia mensagem via `RabbitMQMessageService`
6. Retorna resposta via `ResponseBuilder`

#### HeaderValidator

**Responsabilidade**: Validar todos os headers obrigatórios da requisição.

**Validações**:
- `origin`: Não pode ser vazio
- `destination`: Não pode ser vazio
- `path`: Não pode ser vazio
- `operation`: Não pode ser vazio
- `userID`: Não pode ser vazio
- `applicationMode`: Deve ser `TRANSMITTER` ou `RECEIVER`

**Retorno**: `ValidationResult` com status e mensagem de erro (se houver)

### Application Layer

#### RabbitMQMessageService

**Responsabilidade**: Abstração da camada de aplicação para operações de mensageria.

**Funcionalidades**:
- Validação de mensagens antes de publicar
- Tratamento de exceções específicas do RabbitMQ
- Logging estruturado
- Suporte a mensagens simples e com headers customizados

**Métodos Principais**:
- `sendMessage(String message)`: Envia mensagem simples
- `sendMessageWithHead(String message, RequestHeadersDTO headers)`: Envia mensagem com headers

#### JsonPayloadParser

**Responsabilidade**: Parsing e validação de payloads JSON.

**Funcionalidades**:
- Aceita body vazio/null (retorna `{}`)
- Valida formato JSON
- Converte para `JsonNode` para manipulação
- Serializa de volta para String quando necessário

**Tratamento Especial**:
- Body `null` → `{}`
- Body vazio → `{}`
- JSON inválido → Exceção com mensagem clara

#### RequestHeadersBuilder

**Responsabilidade**: Construir DTO de headers com informações de rastreabilidade.

**Funcionalidades**:
- Geração automática de `correlationID` se não fornecido
- Geração automática de `timestamp` ISO-8601
- Extração de headers customizados
- Validação de headers obrigatórios

#### ExternalApiClient

**Responsabilidade**: Comunicação HTTP com APIs externas.

**Funcionalidades**:
- Envio de requisições POST com JSON
- Tratamento de erros HTTP (4xx, 5xx)
- Timeout configurável
- Logging de performance (duração das requisições)
- Retry automático (configurável)

**Tratamento de Erros**:
- `ResourceAccessException`: Erro de conexão/rede
- `HttpClientErrorException`: Erros 4xx
- `HttpServerErrorException`: Erros 5xx
- `RestClientException`: Outros erros REST

### Infrastructure Layer

#### RabbitMQMessagePublisher

**Responsabilidade**: Publicação de mensagens no RabbitMQ.

**Funcionalidades**:
- Criação de mensagens com headers
- Configuração de propriedades de mensagem
- Publicação em filas específicas
- Tratamento de erros de conexão

#### RabbitListener

**Responsabilidade**: Consumo de mensagens do RabbitMQ.

**Funcionalidades**:
- Escuta fila de saída (`data.anonymization.output.queue`)
- Processamento assíncrono via thread pool
- Extração de correlation ID dos headers
- Encaminhamento para API externa
- Tratamento de exceções de negócio e técnicas

**Configuração**:
- Thread pool fixo: 5 threads
- Processamento assíncrono
- Acknowledge automático

#### RabbitMQConfig

**Responsabilidade**: Configuração de beans do RabbitMQ.

**Beans Configurados**:
- `CachingConnectionFactory`: Factory de conexões
- `RabbitTemplate`: Template para publicação
- `Queue`: Fila de saída (output queue)
- `SimpleRabbitListenerContainerFactory`: Factory para listeners

**Configurações**:
- Cache mode: `CHANNEL`
- Channel cache size: 25
- Connection name: `mop-client-gateway`

### Shared Layer

#### MessageBuilderHelper

**Responsabilidade**: Construção de mensagens RabbitMQ com headers.

**Funcionalidades**:
- Geração de correlation IDs rastreáveis
- Construção de `MessageProperties` com headers
- Conversão de DTOs para mensagens RabbitMQ
- Preservação de case dos headers

#### CorrelationIdContext

**Responsabilidade**: Gerenciamento de correlation IDs no contexto da thread.

**Funcionalidades**:
- Armazenamento thread-local de correlation ID
- Integração com MDC para logging
- Limpeza automática após processamento

#### GlobalExceptionHandler

**Responsabilidade**: Tratamento global de exceções.

**Tratamento**:
- `BusinessException`: Erros de negócio (400)
- `ErrorResponseException`: Erros de API externa (500)
- `RabbitMQException`: Erros de mensageria (500)
- `IllegalArgumentException`: Validação (400)
- Exceções genéricas (500)

---

## Fluxo de Dados Detalhado

### Fluxo de Requisição HTTP → RabbitMQ

```
1. Cliente HTTP
   │
   ├─ POST /v1/anonymize/data
   ├─ Headers: origin, destination, path, operation, userID, applicationMode
   └─ Body: JSON (opcional)
   │
   ▼
2. AnonymizerController
   │
   ├─ Valida headers (HeaderValidator)
   ├─ Parse JSON (JsonPayloadParser)
   ├─ Constrói RequestHeadersDTO (RequestHeadersBuilder)
   │   ├─ Gera correlationID (se não fornecido)
   │   └─ Gera timestamp ISO-8601
   │
   ▼
3. RabbitMQMessageService
   │
   ├─ Valida mensagem
   ├─ Converte para MessageHeadersDTO
   └─ Delega para RabbitMQMessagePublisher
   │
   ▼
4. RabbitMQMessagePublisher
   │
   ├─ Constrói Message com headers (MessageBuilderHelper)
   ├─ Configura MessageProperties
   └─ Publica na fila: data.anonymization.input.queue
   │
   ▼
5. RabbitMQ
   │
   └─ Mensagem armazenada na fila
```

### Fluxo de Consumo RabbitMQ → API Externa

```
1. RabbitMQ
   │
   └─ Mensagem na fila: data.anonymization.output.queue
   │
   ▼
2. RabbitListener
   │
   ├─ Recebe mensagem (@RabbitListener)
   ├─ Extrai correlationID dos headers
   ├─ Define no MDC para logging
   └─ Submete para thread pool
   │
   ▼
3. Thread Pool (ExecutorService)
   │
   └─ Processa assincronamente
   │
   ▼
4. processMessageSafely()
   │
   ├─ Valida mensagem (não nula, não vazia)
   ├─ Extrai correlationID
   └─ Chama processMessage()
   │
   ▼
5. ExternalApiClient
   │
   ├─ Cria HttpEntity com JSON
   ├─ Envia POST para API externa
   ├─ Monitora duração
   └─ Trata erros HTTP
   │
   ▼
6. API Externa
   │
   └─ Processa requisição
```

### Fluxo Completo do Ecossistema

```
┌─────────────┐
│   Cliente   │
└──────┬──────┘
       │ HTTP POST
       ▼
┌─────────────────┐
│     Gateway     │
│  (Este serviço) │
└──────┬──────────┘
       │ RabbitMQ
       ▼
┌─────────────────┐
│  data.anonymi-  │
│  zation.input   │
│  .queue         │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  Anonymization  │
│     Service     │
└──────┬──────────┘
       │ RabbitMQ
       ▼
┌─────────────────┐
│  data.anonymi-  │
│  zation.output  │
│  .queue         │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  Validator      │
│     Service     │
└──────┬──────────┘
       │ RabbitMQ
       ▼
┌─────────────────┐
│  data.validator │
│  .input.queue   │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│     Gateway     │
│  (Consome via   │
│   RabbitListener│
└─────────────────┘
```

---

## Configuração Avançada

### Spring Profiles

#### Profile `local`

**Uso**: Desenvolvimento local

**Características**:
- Todos os valores têm defaults
- RabbitMQ em `localhost:5672`
- Credenciais: `guest/guest`
- Logging em nível DEBUG
- Spring DevTools habilitado

**Ativação**:
```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

#### Profile `dev`

**Uso**: Ambiente de desenvolvimento

**Características**:
- Sem valores default
- Todas as variáveis devem ser fornecidas
- Configurações via variáveis de ambiente
- Logging em nível INFO

#### Profile `homolog`

**Uso**: Ambiente de homologação

**Características**:
- Sem valores default
- Configurações via variáveis de ambiente
- Logging em nível INFO
- Validações mais rigorosas

### Variáveis de Ambiente Críticas

#### RabbitMQ

```bash
RABBITMQ_VALIDATOR_HOST=localhost          # Host do RabbitMQ
RABBITMQ_VALIDATOR_PORT=5672               # Porta AMQP
RABBITMQ_USERNAME=guest                    # Usuário
RABBITMQ_PASSWORD=guest                    # Senha
RABBITMQ_VALIDATOR_QUEUE_NAME=data.anonymization.input.queue
RABBITMQ_OUTPUT_QUEUE_NAME=data.anonymization.output.queue
```

#### Performance

```bash
RABBITMQ_CONCURRENCY=1                     # Consumidores iniciais
RABBITMQ_MAX_CONCURRENCY=5                 # Máximo de consumidores
RABBITMQ_PREFETCH=10                       # Mensagens pré-buscadas
RABBITMQ_RETRY_MAX_ATTEMPTS=5              # Tentativas de retry
RABBITMQ_RETRY_BACKOFF=2000                # Delay entre retries (ms)
```

#### API Externa

```bash
EXTERNAL_REQUEST_URL=http://api.example.com/process
```

### Configuração de Logging

O projeto usa **Logback** com configuração em `logback-spring.xml`.

**Níveis de Log**:
- `DEBUG`: Informações detalhadas (desenvolvimento)
- `INFO`: Informações gerais (produção)
- `WARN`: Avisos
- `ERROR`: Erros

**Configuração por Profile**:
```yaml
logging:
  level:
    root: INFO
    br.com.opin.mopclient.gateway: DEBUG  # Profile local
```

---

## RabbitMQ - Configuração e Uso

### Filas Utilizadas

#### Fila de Entrada (Produtor)

**Nome**: `data.anonymization.input.queue`

**Uso**: Gateway publica mensagens aqui

**Características**:
- Durable: `true`
- Auto-delete: `false`
- Criada automaticamente se não existir

#### Fila de Saída (Consumidor)

**Nome**: `data.anonymization.output.queue`

**Uso**: Gateway consome mensagens processadas

**Características**:
- Durable: `true`
- Auto-delete: `false`
- Listener configurado com `@RabbitListener`

### Configuração de Conexão

```java
CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
factory.setUsername(username);
factory.setPassword(password);
factory.setCacheMode(CacheMode.CHANNEL);
factory.setChannelCacheSize(25);
factory.setConnectionNameStrategy(f -> "mop-client-gateway");
```

### Configuração de Listener

```java
SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
factory.setConnectionFactory(connectionFactory);
factory.setConcurrentConsumers(1);
factory.setMaxConcurrentConsumers(5);
factory.setPrefetchCount(10);
factory.setAutoStartup(true);
```

### Headers de Mensagem

As mensagens RabbitMQ incluem os seguintes headers:

- `origin`: Sistema originador
- `destination`: Sistema destino
- `path`: Caminho lógico
- `operation`: Operação HTTP
- `correlationID`: ID de correlação
- `userID`: ID do usuário
- `applicationMode`: Modo (TRANSMITTER/RECEIVER)
- `timestamp`: Timestamp ISO-8601
- `headers`: Headers customizados (Map)

### Retry e Resiliência

**Configuração de Retry**:
```yaml
spring:
  rabbitmq:
    retry:
      maxAttempts: 5
      backoff: 2000
      enablesTransactionSupport: true
```

**Comportamento**:
- Até 5 tentativas
- Delay exponencial entre tentativas
- Suporte a transações habilitado

---

## Rastreamento e Correlation IDs

### Formato do Correlation ID

```
mop-gateway-YYYYMMdd-HHmmss-SSS-XXXXXXXX
```

**Exemplo**:
```
mop-gateway-20260108-143133-598-6b5c106a
```

**Componentes**:
- `mop-gateway`: Prefixo fixo
- `YYYYMMdd`: Data (20260108)
- `HHmmss`: Hora (143133)
- `SSS`: Milissegundos (598)
- `XXXXXXXX`: Hash hexadecimal (8 caracteres)

### Geração Automática

O correlation ID é gerado automaticamente se não fornecido no header da requisição.

**Localização**: `MessageBuilderHelper.generateTraceableCorrelationId()`

### Rastreamento no MDC

O correlation ID é automaticamente adicionado ao **MDC (Mapped Diagnostic Context)** para logging:

```java
MDC.put("correlationId", correlationId);
```

**Benefícios**:
- Logs estruturados
- Rastreamento distribuído
- Facilita debugging

### Preservação em Mensagens RabbitMQ

O correlation ID é preservado em todas as mensagens RabbitMQ através do header `correlationID`.

---

## Tratamento de Erros

### Hierarquia de Exceções

```
Exception
├── BusinessException          # Erros de negócio
├── ErrorResponseException     # Erros de API externa
├── RabbitMQException         # Erros de mensageria
└── IllegalArgumentException   # Erros de validação
```

### Tratamento por Tipo

#### BusinessException

**Uso**: Erros de regra de negócio

**HTTP Status**: `400 Bad Request`

**Exemplo**:
```json
{
  "error": "Business error",
  "details": "Invalid operation for this context"
}
```

#### ErrorResponseException

**Uso**: Erros ao chamar API externa

**HTTP Status**: `500 Internal Server Error`

**Tipos**:
- Connection error: Não conseguiu conectar
- Client error: HTTP 4xx
- Server error: HTTP 5xx
- Request error: Outros erros REST

#### RabbitMQException

**Uso**: Erros ao publicar/consumir mensagens

**HTTP Status**: `500 Internal Server Error`

**Causas Comuns**:
- Conexão perdida com RabbitMQ
- Fila não existe
- Permissões insuficientes

#### IllegalArgumentException

**Uso**: Validação de entrada

**HTTP Status**: `400 Bad Request`

**Exemplo**:
```json
{
  "error": "Invalid header",
  "details": "Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'"
}
```

### GlobalExceptionHandler

Todas as exceções são capturadas pelo `GlobalExceptionHandler` e convertidas em respostas JSON padronizadas.

---

## Performance e Otimizações

### Thread Pool

O `RabbitListener` usa um thread pool fixo de **5 threads** para processamento assíncrono:

```java
ExecutorService executorService = Executors.newFixedThreadPool(5);
```

**Considerações**:
- Aumentar se houver alta carga
- Monitorar uso de CPU e memória
- Ajustar baseado em métricas

### RabbitMQ Connection Pooling

**Cache Mode**: `CHANNEL`

**Channel Cache Size**: 25

**Benefícios**:
- Reutilização de conexões
- Redução de overhead
- Melhor performance

### RestTemplate Timeouts

```java
RestTemplate restTemplate = builder
    .setConnectTimeout(Duration.ofSeconds(5))
    .setReadTimeout(Duration.ofSeconds(10))
    .build();
```

**Configuração**:
- Connect timeout: 5 segundos
- Read timeout: 10 segundos

### Lazy Initialization

A aplicação usa lazy initialization para melhorar tempo de startup:

```java
application.setLazyInitialization(true);
```

---

## Segurança

### Headers Sensíveis

**NÃO são logados**:
- `spring.rabbitmq.password`
- `spring.rabbitmq.username` (em alguns contextos)

O `ApplicationPropertiesLogger` redacta informações sensíveis.

### Validação de Entrada

Todas as entradas são validadas:
- Headers obrigatórios
- Formato de JSON
- Valores permitidos (ex: applicationMode)

### Content-Type

Apenas `application/json` é aceito.

### Correlation ID

O correlation ID é gerado de forma segura usando:
- Timestamp
- Hash aleatório
- Formato padronizado

---

## Monitoramento e Observabilidade

### Spring Boot Actuator

**Endpoints Disponíveis**:
- `/actuator/health`: Health check
- `/actuator/info`: Informações da aplicação
- `/actuator/metrics`: Métricas

**Configuração**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
```

### Logs Estruturados

Todos os logs incluem:
- Correlation ID (via MDC)
- Timestamp
- Nível de log
- Classe e método

### Métricas Importantes

- Tempo de resposta de API externa
- Taxa de sucesso/erro
- Número de mensagens processadas
- Tamanho das filas RabbitMQ

### ApplicationStartupListener

Verifica e loga o status de todos os componentes na inicialização:
- RabbitMQ Connection Factory
- Filas
- Templates
- Listeners
- Cache Manager
- RestTemplate
- External API Client

---

## Desenvolvimento e Contribuição

### Estrutura de Código

```
src/main/java/br/com/opin/mopclient/gateway/
├── application/          # Lógica de negócio
│   ├── service/         # Serviços de aplicação
│   └── usecase/         # Casos de uso
├── infrastructure/       # Infraestrutura
│   ├── adapter/        # Adaptadores externos
│   ├── config/         # Configurações
│   ├── filter/        # Filtros HTTP
│   └── interceptor/    # Interceptadores
├── interfaces/          # Interface com usuário
│   ├── controller/     # Controllers REST
│   ├── dto/           # Data Transfer Objects
│   └── validation/    # Validadores
└── shared/             # Código compartilhado
    ├── dto/           # DTOs compartilhados
    ├── exception/     # Exceções
    └── util/          # Utilitários
```

### Convenções de Código

- **Nomes de classes**: PascalCase
- **Nomes de métodos**: camelCase
- **Nomes de constantes**: UPPER_SNAKE_CASE
- **Logs**: Em inglês
- **Comentários**: Em português (quando necessário)

### Testes

**Estrutura**:
```
src/test/java/br/com/opin/mopclient/gateway/
```

**Tipos de Teste**:
- Unitários: Serviços e utilitários
- Integração: Controllers e adaptadores
- Componentes: Fluxos completos

### Build e Deploy

**Build**:
```bash
mvn clean package
```

**Testes**:
```bash
mvn test
```

**Skip Tests**:
```bash
mvn clean package -DskipTests
```

---

## Troubleshooting Avançado

### Problema: Mensagens não são consumidas

**Sintomas**:
- Mensagens acumulando na fila
- Logs não mostram processamento

**Diagnóstico**:
1. Verificar se `RabbitListener` está ativo
2. Verificar logs de inicialização
3. Verificar conexão com RabbitMQ
4. Verificar configuração da fila

**Solução**:
```bash
# Verificar logs
docker logs mop-gateway | grep RabbitListener

# Verificar fila no RabbitMQ
curl http://localhost:15672/api/queues/%2F/data.anonymization.output.queue
```

### Problema: Erros ao publicar mensagens

**Sintomas**:
- Erro 500 ao enviar requisição
- Exceção `RabbitMQException`

**Diagnóstico**:
1. Verificar conexão com RabbitMQ
2. Verificar se a fila existe
3. Verificar credenciais
4. Verificar permissões

**Solução**:
```bash
# Verificar conexão
telnet localhost 5672

# Verificar fila
rabbitmqctl list_queues

# Verificar usuário
rabbitmqctl list_users
```

### Problema: Timeout ao chamar API externa

**Sintomas**:
- Erro `ResourceAccessException`
- Timeout após 10 segundos

**Diagnóstico**:
1. Verificar se API externa está acessível
2. Verificar timeout configurado
3. Verificar rede/firewall

**Solução**:
```bash
# Testar conectividade
curl -v http://api-externa.com/process

# Aumentar timeout (se necessário)
# Em RestTemplateConfig
.setReadTimeout(Duration.ofSeconds(30))
```

### Problema: Correlation ID não aparece nos logs

**Sintomas**:
- Logs sem correlation ID
- Dificuldade para rastrear requisições

**Diagnóstico**:
1. Verificar se `CorrelationIdFilter` está ativo
2. Verificar se MDC está configurado
3. Verificar formato de logs

**Solução**:
```xml
<!-- logback-spring.xml -->
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n</pattern>
```

### Problema: Aplicação não inicia

**Sintomas**:
- Erro ao iniciar Spring Boot
- Componentes não carregados

**Diagnóstico**:
1. Verificar logs de inicialização
2. Verificar `ApplicationStartupListener`
3. Verificar configurações obrigatórias

**Solução**:
```bash
# Verificar logs completos
mvn spring-boot:run > startup.log 2>&1

# Verificar componentes carregados
grep "initialized successfully" startup.log
```

---

## Conclusão

Este wiki fornece uma visão detalhada do **MOP Client Gateway**, cobrindo desde arquitetura até troubleshooting avançado. Para mais informações, consulte:

- **README.md**: Guia rápido de uso
- **Código-fonte**: Documentação inline
- **Testes**: Exemplos de uso

---

## Referências e Documentação Adicional

### Documentação Oficial do MOP

- **Módulo de Operações OPIN (MOP)**: Documento de apresentação - Setembro 2024
- **Open Finance Brasil**: Instrução normativa BCB 441
- **Arquitetura MQD**: Área do Desenvolvedor - Open Finance Brasil
- **Arquitetura Drex**: Documentação oficial

### Especificações Técnicas

- **Open Insurance Brasil**: Regulamentação e especificações
- **FAPI OP/RP**: Especificações de certificação
- **ICP-Brasil**: Certificados digitais

### Materiais de Referência

- Open Finance Brasil - Arquitetura e especificações
- Open Insurance Brasil - Regulamentação
- Documentação técnica do ecossistema OPIN

---

**Última atualização**: Janeiro 2026

**Confidencialidade**: Este documento contém informações técnicas sobre o MOP Client Gateway. Para informações sobre o ecossistema MOP completo, consulte a documentação oficial do Open Insurance Brasil.

