# MOP Client

Gateway responsável por receber requisições de anonimização e encaminhá-las para processamento via RabbitMQ no ecossistema Open Insurance.

## Público-Alvo

Esta documentação é destinada a desenvolvedores e equipes técnicas responsáveis pela integração, operação e manutenção do MOP Client no ecossistema Open Insurance.

## 📋 Índice

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

O **Módulo de Operações OPIN (MOP)** promove transparência e confiabilidade no Open Insurance, facilitando comunicação entre sistemas e garantindo qualidade dos dados.

**Objetivos:**
- Facilitar acesso direto à infraestrutura dos participantes
- Eliminar obstáculos na comunicação entre sistemas
- Acelerar implementação e análise de qualidade dos dados
- Prevenir riscos de cibersegurança e fraudes

> 📖 Para mais detalhes sobre arquitetura e fluxo de mensagens, consulte [ARQUITETURA.md](docs/ARQUITETURA.md)

---

## Início Rápido

### Pré-requisitos
- Java 17+
- Maven 3.x
- Docker e Docker Compose (para RabbitMQ)

### Passos

1. **Iniciar RabbitMQ:**
```bash
docker-compose up -d
```

> 💡 **Dica**: Para mais informações sobre RabbitMQ, consulte [RABBITMQ.md](docs/RABBITMQ.md)

2. **Executar aplicação:**
```bash
mvn spring-boot:run
```

3. **Testar endpoint:**
```bash
curl -X POST http://localhost:8080/v1/anonymize/data \
  -H "origin: Sistema" \
  -H "destination: Sistema" \
  -H "path: /test" \
  -H "operation: POST" \
  -H "userID: user123" \
  -H "applicationMode: TRANSMITTER" \
  -H "Content-Type: application/json"
```

---

## API

### Endpoint

**POST** `/v1/anonymize/data`

Recebe requisições de anonimização e encaminha para processamento via RabbitMQ.

**Características:**
- ✅ Body **opcional** (vazio/null/inválido = `{}`)
- ✅ Gera `correlationID` e `timestamp` automaticamente
- ✅ Atualmente suporta apenas `Content-Type: application/json`
- ✅ Retorna JSON estruturado com informações de traceability

### Headers Obrigatórios

| Header | Descrição | Exemplo |
|--------|-----------|---------|
| `origin` | Sistema originador | `Sistema` |
| `destination` | Destino esperado | `Sistema` |
| `path` | Rota alvo (logs) | `/open-insurance/consents/v2/consents` |
| `operation` | Método da operação | `POST`, `GET`, `PUT`, `DELETE`, `PROCESS` |
| `userID` | ID do usuário/sistema | `user-12345` |
| `applicationMode` | Modo: `TRANSMITTER` ou `RECEIVER` | `TRANSMITTER` |

### Headers Opcionais

| Header | Descrição |
|--------|-----------|
| `correlationID` | ID de correlação (gerado automaticamente se não fornecido) |

### Respostas

**Sucesso (200):**
```json
{
  "status": "SUCCESS",
  "message": "Request processed successfully. Your data has been received and forwarded to the queue.",
  "correlationId": "mop-gateway-20240115-143025-123-abc12345",
  "timestamp": "2024-01-15T14:30:25.123Z",
  "origin": "Sistema",
  "destination": "Sistema",
  "path": "/open-insurance/consents/v2/consents",
  "operation": "POST",
  "applicationMode": "TRANSMITTER"
}
```

**Erro de Validação (400):**
```json
{
  "status": "ERROR",
  "error": "Invalid header",
  "details": "Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'",
  "timestamp": "2024-01-15T14:30:25.123Z"
}
```

**Erro de Processamento (500):**
```json
{
  "status": "ERROR",
  "error": "Message processing error",
  "details": "Failed to process message: [detalhes]",
  "timestamp": "2024-01-15T14:30:25.123Z"
}
```

---

## Configuração

### Profiles de Ambiente

A aplicação suporta diferentes profiles para diferentes ambientes:

| Profile | Arquivo | Descrição | Valores Default |
|---------|---------|-----------|-----------------|
| `local` | `application-local.yml` | Desenvolvimento local | ✅ Sim |
| `dev` | `application-dev.yml` | Ambiente de desenvolvimento | ❌ Não |
| `homolog` | `application-homolog.yml` | Ambiente de homologação | ❌ Não |

**Ativação:**
```bash
export SPRING_PROFILES_ACTIVE=dev
# ou
java -jar app.jar --spring.profiles.active=dev
# ou
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> ⚠️ **Importante**: Profiles `dev` e `homolog` **não possuem valores default**. Todas as variáveis de ambiente devem ser configuradas.

### Variáveis de Ambiente Essenciais

Para os profiles `dev` e `homolog`, as seguintes variáveis são **obrigatórias**:

| Variável | Descrição |
|----------|-----------|
| `SPRING_PROFILES_ACTIVE` | Profile ativo (`dev`, `homolog`, `local`) |
| `RABBITMQ_VALIDATOR_HOST` | Host do RabbitMQ |
| `RABBITMQ_USERNAME` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ |
| `EXTERNAL_REQUEST_URL` | URL da API externa |

> 💡 **Nota**: O profile `local` possui valores padrão para todas as variáveis, permitindo execução sem configuração adicional.

📌 **A lista completa de variáveis está disponível em:** [docs/VARIAVEIS_DE_AMBIENTE.md](docs/VARIAVEIS_DE_AMBIENTE.md)

---

## Execução

### Local (Maven)

```bash
# Com profile local (padrão)
mvn spring-boot:run

# Com profile específico
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> 💡 **Dica**: O RabbitMQ deve estar rodando. Consulte [RABBITMQ.md](docs/RABBITMQ.md) para mais informações.

### Docker

Para executar o ecossistema MOP Client via Docker (Gateway, Anonymization e Validator), consulte a documentação completa em [EXECUCAO_DOCKER.md](docs/EXECUCAO_DOCKER.md).

**Resumo rápido:**

1. Inicie o RabbitMQ: `docker-compose up -d`
2. Baixe as imagens do registry
3. Execute os containers na ordem: Gateway → Anonymization → Validator

Para comandos detalhados, configurações e troubleshooting, acesse [docs/EXECUCAO_DOCKER.md](docs/EXECUCAO_DOCKER.md).

---

## Exemplos

### Requisição com Body Vazio

```bash
curl -X POST http://localhost:8080/v1/anonymize/data \
  -H "origin: Sistema" \
  -H "destination: Sistema" \
  -H "path: /open-insurance/consents/v2/consents" \
  -H "operation: POST" \
  -H "userID: user-12345" \
  -H "applicationMode: TRANSMITTER" \
  -H "Content-Type: application/json"
```

**Resposta:**
```json
{
  "status": "SUCCESS",
  "message": "Request processed successfully. Your data has been received and forwarded to the queue.",
  "correlationId": "mop-gateway-20240115-143025-123-abc12345",
  "timestamp": "2024-01-15T14:30:25.123Z",
  "origin": "Sistema",
  "destination": "Sistema",
  "path": "/open-insurance/consents/v2/consents",
  "operation": "POST",
  "applicationMode": "TRANSMITTER"
}
```

### Requisição com Payload JSON

```bash
curl -X POST http://localhost:8080/v1/anonymize/data \
  -H "origin: Sistema" \
  -H "destination: Sistema" \
  -H "path: /open-insurance/consents/v2/consents" \
  -H "operation: POST" \
  -H "userID: user-12345" \
  -H "applicationMode: TRANSMITTER" \
  -H "Content-Type: application/json" \
  -d '{"data": {"key": "value"}}'
```

### Com CorrelationID Customizado

```bash
curl -X POST http://localhost:8080/v1/anonymize/data \
  -H "origin: Sistema" \
  -H "destination: Sistema" \
  -H "path: /test" \
  -H "operation: POST" \
  -H "userID: user123" \
  -H "applicationMode: TRANSMITTER" \
  -H "correlationID: custom-correlation-id-123" \
  -H "Content-Type: application/json"
```

---

## Documentação Completa

Para informações detalhadas, consulte os guias específicos:

- **[Arquitetura](docs/ARQUITETURA.md)** - Diagrama e explicação completa da arquitetura
- **[Execução via Docker](docs/EXECUCAO_DOCKER.md)** - Passo a passo detalhado para execução via Docker
- **[Variáveis de Ambiente](docs/VARIAVEIS_DE_AMBIENTE.md)** - Tabela completa de variáveis de ambiente
- **[Mensageria](docs/MENSAGERIA.md)** - Filas, retry, DLQ e configuração RabbitMQ
- **[Configuração RabbitMQ](docs/RABBITMQ.md)** - Guia de configuração e uso do RabbitMQ
- **[FAQ](docs/FAQ.md)** - Erros comuns e troubleshooting

---

## Referências

### Repositórios do Ecossistema MOP

- **[MOP Client](https://github.com/br-openinsurance/opin-mop-gateway-pub)** - Este repositório
- **[MOP Client Data Validator](https://github.com/br-openinsurance/mop-client-data-validator-pub)** - Serviço de validação
- **[MOP Client Anonymization](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub)** - Serviço de anonimização

### Documentação Técnica

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/html/)
- [Open Insurance Brasil](https://www.gov.br/susep/pt-br/assuntos/open-insurance)
