# Mensageria RabbitMQ

Este documento descreve a configuração e uso do RabbitMQ no MOP Client, incluindo filas, retry e Dead Letter Queue (DLQ).

## Filas do Ecossistema MOP

O ecossistema MOP utiliza as seguintes filas para comunicação assíncrona:

| Fila | Descrição | Produtor | Consumidor |
|------|-----------|----------|------------|
| `data.anonymization.input.queue` | Dados brutos para anonimização | Gateway | [Anonymization Service](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub) |
| `data.anonymization.output.queue` | Dados anonimizados | [Anonymization Service](https://github.com/br-openinsurance/opin-mop-client-anonymization-pub) | [Validator Service](https://github.com/br-openinsurance/mop-client-data-validator-pub) |
| `data.validator.input.queue` | Payloads validados | [Validator Service](https://github.com/br-openinsurance/mop-client-data-validator-pub) | Gateway |

## Fluxo de Mensagens

```
Gateway → data.anonymization.input.queue → Anonymization Service
                                                                  ↓
Gateway ← data.validator.input.queue ← Validator Service ← data.anonymization.output.queue
```

## Configuração das Filas

### Configuração Recomendada

Para ambientes de produção, recomenda-se:

- **Durabilidade**: `durable=true`
  - Filas sobrevivem a reinicializações do RabbitMQ
  - Mensagens não são perdidas em caso de falha

- **Auto-delete**: `false`
  - Filas não são removidas quando não há consumidores
  - Permite resiliência e reconexão

- **Formato**: JSON com `payload` e headers de rastreamento
  - Estrutura padronizada para todas as mensagens
  - Facilita rastreamento e debugging

### Headers de Rastreamento

Todas as mensagens incluem os seguintes headers para rastreamento:

- `correlationId`: ID de correlação único
- `timestamp`: Timestamp ISO-8601
- `origin`: Sistema originador
- `destination`: Destino esperado
- `path`: Rota alvo
- `operation`: Método da operação
- `applicationMode`: Modo (TRANSMITTER/RECEIVER)

## Retry (Tentativas de Retry)

### Configuração

O sistema possui configuração de retry para lidar com falhas temporárias:

| Configuração | Propriedade | Padrão | Descrição |
|--------------|-------------|--------|-----------|
| `maxAttempts` | `RABBITMQ_RETRY_MAX_ATTEMPTS` | `5` | Número máximo de tentativas |
| `backoff` | `RABBITMQ_RETRY_BACKOFF` | `2000` ms | Delay entre tentativas (milissegundos) |
| `enablesTransactionSupport` | `RABBITMQ_ENABLES_TRANSACTION_SUPPORT` | `true` | Suporte a transações |

### Comportamento

1. **Primeira tentativa**: Processamento imediato
2. **Falha**: Aguarda `backoff` ms antes da próxima tentativa
3. **Tentativas subsequentes**: Até `maxAttempts` vezes
4. **Excedido maxAttempts**: Mensagem enviada para DLQ (se configurada)

### Exemplo de Configuração

```yaml
spring:
  rabbitmq:
    retry:
      maxAttempts: 5
      backoff: 2000
      enablesTransactionSupport: true
```

## Dead Letter Queue (DLQ)

### Visão Geral

Dead Letter Queue é uma fila especial para mensagens que não puderam ser processadas após todas as tentativas de retry.

### Configuração

Para configurar DLQ, é necessário:

1. **Criar exchange para DLQ**:
```bash
rabbitmqadmin declare exchange name=dlx type=direct
```

2. **Criar fila de DLQ**:
```bash
rabbitmqadmin declare queue name=data.anonymization.input.queue.dlq durable=true
```

3. **Binding entre exchange e fila**:
```bash
rabbitmqadmin declare binding source=dlx destination=data.anonymization.input.queue.dlq routing_key=data.anonymization.input.queue
```

### Benefícios

- **Isolamento de erros**: Mensagens com erro não bloqueiam outras mensagens
- **Análise posterior**: Permite investigar mensagens problemáticas
- **Recuperação**: Possibilidade de reprocessamento manual

### Monitoramento

Mensagens na DLQ devem ser monitoradas e investigadas:

- Volume de mensagens na DLQ
- Padrões de erro
- Necessidade de ajustes na aplicação

## Configuração de Consumidores

### Concorrência

| Configuração | Propriedade | Padrão | Descrição |
|--------------|-------------|--------|-----------|
| `concurrency` | `RABBITMQ_CONCURRENCY` | `1` | Número inicial de consumidores |
| `max-concurrency` | `RABBITMQ_MAX_CONCURRENCY` | `5` | Máximo de consumidores |
| `prefetch` | `RABBITMQ_PREFETCH` | `10` | Mensagens pré-buscadas por consumidor |

### Acknowledge Mode

O sistema utiliza `acknowledge-mode: auto`, onde:

- Mensagens são automaticamente confirmadas após processamento bem-sucedido
- Mensagens com erro são rejeitadas e retornam à fila (ou vão para DLQ)

## Troubleshooting

### Mensagens não são consumidas

1. Verificar se os consumidores estão ativos
2. Verificar conectividade com RabbitMQ
3. Verificar logs de erro
4. Verificar configuração de filas

### Mensagens na DLQ

1. Analisar logs para identificar causa do erro
2. Verificar formato das mensagens
3. Verificar se dependências externas estão disponíveis
4. Considerar reprocessamento manual

### Performance

1. Ajustar `prefetch` para melhor throughput
2. Aumentar `concurrency` se necessário
3. Monitorar uso de recursos

## Referências

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/html/)
- [RabbitMQ Best Practices](https://www.rabbitmq.com/best-practices.html)

