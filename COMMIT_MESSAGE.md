feat: Implementa AnonymizerController original e ajusta logs de inicialização

Implementa o controller AnonymizerController exatamente como no gateway antigo,
mantendo a mesma estrutura de entrada, validações e tratamento de exceções.
Adiciona logs de inicialização completos e corrige processamento de requisições.

## Mudanças Principais

### Controller e API
- Cria AnonymizerController com estrutura idêntica ao gateway antigo
- Endpoint POST /data com headers obrigatórios (origin, destination, path, operation, applicationMode)
  - Validação de headers via HeaderValidator
  - Parse de JSON via JsonPayloadParser
  - Validação OpenAPI + anonimização síncrona
  - Tratamento de exceções (JsonProcessingException, Exception genérica)
  - Logs no formato original (LOGGER em maiúsculas)

### DTOs e Serviços
- Cria ApiResponseDTO para respostas da API
- Cria RequestHeadersDTO para headers de requisição
- Cria HeaderValidator com validação completa de headers obrigatórios
- Cria JsonPayloadParser para parse de JSON (fault-tolerant)
- Cria RequestHeadersBuilder para construção de headers DTO com correlation ID
- Cria ResponseBuilder para construção de respostas de sucesso/erro

### Logs e Inicialização
- Cria ApplicationStartupListener para logs de inicialização
  - Verifica Cache Manager, OpenAPI Specification, Validation Service
  - Verifica Validation Use Case, Anonymization Use Case
  - Verifica RestTemplate e External API Client
  - Exibe status de todos os componentes na inicialização
- Ajusta logs do RestTemplateConfigRepository para indicar quando URL não está configurada

### Configurações
- Adiciona configuração Jackson no application.yml (fail-on-unknown-properties: false)
- Atualiza ObjectMapperConfig para configurar MappingJackson2HttpMessageConverter
- Ajusta ProcessRequest para aceitar campo "data" via @JsonAlias

## Estrutura Mantida
- Mesmas constantes de headers (ORIGIN, DESTINATION, PATH, OPERATION, USERID, APPLICATION_MODE)
- Mesma estrutura de validação e tratamento de erros
- Mesmos logs e mensagens de resposta
- Headers obrigatórios (required = true)
- MediaType.APPLICATION_JSON_VALUE

## Adaptações para Fluxo Síncrono
- Substitui RabbitMQMessageService por ValidationUseCase + AnonymizeDataUseCase
- Adiciona validação OpenAPI antes da anonimização
- Mantém processamento síncrono (request → validation → anonymization → response)
- Remove dependências de RabbitMQ mantendo compatibilidade com API original

## Arquivos Criados
- src/main/java/br/com/openinsurance/mop/gateway/api/controller/AnonymizerController.java
- src/main/java/br/com/openinsurance/mop/gateway/api/dto/ApiResponseDTO.java
- src/main/java/br/com/openinsurance/mop/gateway/api/dto/RequestHeadersDTO.java
- src/main/java/br/com/openinsurance/mop/gateway/api/validation/HeaderValidator.java
- src/main/java/br/com/openinsurance/mop/gateway/api/service/JsonPayloadParser.java
- src/main/java/br/com/openinsurance/mop/gateway/api/service/RequestHeadersBuilder.java
- src/main/java/br/com/openinsurance/mop/gateway/api/service/ResponseBuilder.java
- src/main/java/br/com/openinsurance/mop/gateway/config/ApplicationStartupListener.java

## Arquivos Modificados
- src/main/java/br/com/openinsurance/mop/gateway/api/request/ProcessRequest.java
- src/main/java/br/com/openinsurance/mop/gateway/api/controller/MopProcessController.java
- src/main/java/br/com/openinsurance/mop/gateway/anonymization/infrastructure/RestTemplateConfigRepository.java
- src/main/java/br/com/openinsurance/mop/gateway/config/ObjectMapperConfig.java
- src/main/resources/application.yml

