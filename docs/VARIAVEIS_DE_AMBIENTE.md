# Variáveis de Ambiente

Este documento lista todas as variáveis de ambiente disponíveis para configuração do MOP Client.

## Para Profiles `dev` e `homolog`

Todas as variáveis abaixo são **obrigatórias** quando usando os profiles `dev` ou `homolog`. O profile `local` possui valores padrão para todas elas.

## Servidor e RabbitMQ

| Variável | Propriedade Spring | Descrição |
|----------|-------------------|-----------|
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | Profile ativo (`dev`, `homolog`, `local`) |
| `SERVER_PORT` | `server.port` | Porta HTTP (padrão: `8080`) |
| `RABBITMQ_VALIDATOR_HOST` | `spring.rabbitmq.host` | Host do RabbitMQ |
| `RABBITMQ_VALIDATOR_PORT` | `spring.rabbitmq.port` | Porta do RabbitMQ (padrão: `5672`) |
| `RABBITMQ_USERNAME` | `spring.rabbitmq.username` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `spring.rabbitmq.password` | Senha do RabbitMQ |

## Filas RabbitMQ

| Variável | Propriedade Spring | Descrição |
|----------|-------------------|-----------|
| `RABBITMQ_VALIDATOR_QUEUE_NAME` | `spring.rabbitmq.queues.validator.name` | Fila de validação (padrão: `data.validator.input.queue`) |
| `RABBITMQ_OUTPUT_QUEUE_NAME` | `spring.rabbitmq.queues.output.name` | Fila de saída (padrão: `data.anonymization.output.queue`) |

## Configurações Avançadas (Opcionais)

| Variável | Propriedade Spring | Descrição | Default (local) |
|----------|-------------------|-----------|-----------------|
| `RABBITMQ_CONCURRENCY` | `spring.rabbitmq.listener.simple.concurrency` | Número de consumidores concorrentes | `1` |
| `RABBITMQ_MAX_CONCURRENCY` | `spring.rabbitmq.listener.simple.max-concurrency` | Máximo de consumidores | `5` |
| `RABBITMQ_PREFETCH` | `spring.rabbitmq.listener.simple.prefetch` | Mensagens pré-buscadas por consumidor | `10` |
| `RABBITMQ_RETRY_MAX_ATTEMPTS` | `spring.rabbitmq.retry.maxAttempts` | Tentativas de retry | `5` |
| `RABBITMQ_RETRY_BACKOFF` | `spring.rabbitmq.retry.backoff` | Delay entre retries (ms) | `2000` |
| `RABBITMQ_ENABLES_TRANSACTION_SUPPORT` | `spring.rabbitmq.retry.enablesTransactionSupport` | Suporte a transações | `true` |

## API Externa e Monitoramento

| Variável | Propriedade Spring | Descrição |
|----------|-------------------|-----------|
| `EXTERNAL_REQUEST_URL` | `external.server.request.url` | URL da API externa |
| `SERVER_CONTEXT_PATH` | `server.servlet.context-path` | Context path (padrão: `/v1/anonymize`) |
| `MANAGEMENT_ENDPOINTS_INCLUDE` | `management.endpoints.web.exposure.include` | Endpoints Actuator expostos (padrão: `*`) |
| `MANAGEMENT_HEALTH_SHOW_DETAILS` | `management.endpoint.health.show-details` | Detalhes do health check (padrão: `always`) |
| `LOG_LEVEL_ROOT` | `logging.level.root` | Nível de log raiz (padrão: `INFO`) |
| `LOG_LEVEL_GATEWAY` | `logging.level.br.com.opin.mopclient.gateway` | Nível de log do gateway (padrão: `DEBUG`) |

## Valores Padrão

O profile `local` possui valores padrão para todas as variáveis acima, permitindo execução sem configuração adicional.

Para os profiles `dev` e `homolog`, **todas as variáveis devem ser configuradas**, pois não há valores padrão.

## Exemplos de Uso

### Linux/macOS

```bash
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8080
export RABBITMQ_VALIDATOR_HOST=localhost
export RABBITMQ_VALIDATOR_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest
export EXTERNAL_REQUEST_URL=http://api.example.com/process
```

### Windows PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SERVER_PORT="8080"
$env:RABBITMQ_VALIDATOR_HOST="localhost"
$env:RABBITMQ_VALIDATOR_PORT="5672"
$env:RABBITMQ_USERNAME="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:EXTERNAL_REQUEST_URL="http://api.example.com/process"
```

### Docker

```bash
docker run --rm \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SERVER_PORT=8080 \
  -e RABBITMQ_VALIDATOR_HOST=localhost \
  -e RABBITMQ_VALIDATOR_PORT=5672 \
  -e RABBITMQ_USERNAME=guest \
  -e RABBITMQ_PASSWORD=guest \
  -e EXTERNAL_REQUEST_URL=http://api.example.com/process \
  ghcr.io/br-openinsurance/opin-mop-gateway-pub:develop
```

