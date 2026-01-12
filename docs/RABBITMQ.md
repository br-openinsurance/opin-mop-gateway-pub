# Configuração RabbitMQ

Este documento descreve a configuração e uso do RabbitMQ no contexto do MOP Client.

## Início Rápido

Para iniciar o RabbitMQ localmente usando Docker Compose:

```bash
docker-compose up -d
```

## Management UI

Após iniciar o RabbitMQ, acesse o Management UI:

- **URL**: http://localhost:15672
- **Usuário**: `guest`
- **Senha**: `guest`

O Management UI fornece uma interface web para monitorar filas, conexões, exchanges e mensagens.

## Parar o RabbitMQ

Para parar o RabbitMQ:

```bash
docker-compose down
```

## Portas

O RabbitMQ expõe as seguintes portas:

- **5672**: Porta AMQP para conexões dos clientes
- **15672**: Porta do Management UI

## Configuração do docker-compose.yml

O arquivo `docker-compose.yml` na raiz do projeto configura:

- **Imagem**: `rabbitmq:3-management`
- **Usuário padrão**: `guest`
- **Senha padrão**: `guest`
- **Rede**: `rabbitmq_network` (bridge)

## Filas do Ecossistema MOP

O ecossistema MOP utiliza as seguintes filas:

| Fila | Descrição | Produtor | Consumidor |
|------|-----------|----------|------------|
| `data.anonymization.input.queue` | Dados brutos para anonimização | Gateway | Anonymization Service |
| `data.anonymization.output.queue` | Dados anonimizados | Anonymization Service | Validator Service |
| `data.validator.input.queue` | Payloads validados | Validator Service | Gateway |

## Configuração Recomendada

Para ambientes de produção, recomenda-se:

- **Durabilidade**: `durable=true`
- **Auto-delete**: `false`
- **Dead Letter Queue**: Configurada quando necessário
- **Formato**: JSON com `payload` e headers de rastreamento

## Troubleshooting

### Verificar Status

```bash
docker ps | grep rabbitmq
```

### Ver Logs

```bash
docker logs rabbitmq
```

### Reiniciar

```bash
docker-compose restart rabbitmq
```

### Limpar Dados

⚠️ **Atenção**: Isso remove todas as filas e dados:

```bash
docker-compose down -v
```

## Referências

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/html/)

