# Execução via Docker

Este guia detalha como executar o ecossistema MOP Client via Docker, incluindo Gateway, Anonymization e Validator.

## Pré-requisitos

Antes de executar os containers, certifique-se de que o RabbitMQ está rodando:

```bash
docker-compose up -d
```

Para mais detalhes sobre o RabbitMQ, consulte [RABBITMQ.md](RABBITMQ.md).

## 1. Baixar as Imagens

Baixe as três imagens obrigatórias do registry:

```bash
docker pull ghcr.io/br-openinsurance/opin-mop-gateway-pub:develop
docker pull ghcr.io/br-openinsurance/opin-mop-client-anonymization-pub:develop
docker pull ghcr.io/br-openinsurance/mop-client-data-validator-pub:develop
```

## 2. Executar os Containers

Execute os três containers na seguinte ordem:

### Gateway

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
  ghcr.io/br-openinsurance/opin-mop-gateway-pub:develop
```

### Anonymization

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
  -e EXTERNAL_API_DATA_ANONYMIZATION=${EXTERNAL_API_DATA_ANONYMIZATION} \
  -e EXTERNAL_REQUEST_URL=${EXTERNAL_REQUEST_URL} \
  -e EXTERNAL_REQUEST_HOST=${EXTERNAL_REQUEST_HOST} \
  -e EXTERNAL_REQUEST_PATH=${EXTERNAL_REQUEST_PATH:-/process} \
  -e EXTERNAL_REQUEST_METHOD=${EXTERNAL_REQUEST_METHOD:-POST} \
  -p ${SERVER_PORT_ANONYMIZATION:-8181}:${SERVER_PORT_ANONYMIZATION:-8181} \
  ghcr.io/br-openinsurance/opin-mop-client-anonymization-pub:develop
```

### Validator

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
  ghcr.io/br-openinsurance/mop-client-data-validator-pub:develop
```

## 3. Verificar os Containers

Após iniciar os containers, verifique se estão rodando:

```bash
# Verificar containers rodando
docker ps

# Verificar logs
docker logs mop-gateway
docker logs mop-anonymization
docker logs mop-validator
```

## 4. Testar os Endpoints

```bash
# Health check do Gateway
curl http://localhost:8080/v1/anonymize/actuator/health

# Health check do Anonymization
curl http://localhost:8181/actuator/health

# Health check do Validator
curl http://localhost:8084/actuator/health
```

## Notas

> 💡 **Nota**: Os comandos acima usam variáveis de ambiente com valores padrão. Você pode exportar as variáveis antes de executar os containers para personalizar a configuração. No Windows PowerShell, substitua `\` por `` ` `` ou use aspas.

