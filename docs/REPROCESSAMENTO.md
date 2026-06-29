# Reprocessamento, fila de retry e tempos

Este documento descreve o comportamento do **MOP Client** em relação a **fila de retry**, **resiliência** (circuit breaker, disponibilidade do MOP) e **tempos** configuráveis (cache).

---

## 1. Fluxo HTTP e fila de retry

Cada **POST** `{context-path}/data` executa o processamento **no mesmo processo** (pipeline interno até o envio ao servidor MOP).

Quando o envio ao MOP **não pode ser concluído** de imediato (por exemplo, circuito aberto ou indisponibilidade do serviço), o gateway pode **publicar** o trabalho na fila RabbitMQ configurada em `mop.client.retry.queue` (`MOP_CLIENT_RETRY_QUEUE`). Um agendador de *replay* tenta **reenviar** os itens da fila quando o MOP volta a ser considerado disponível (sondas em `mop.server.availability.*`).

Para o **cliente HTTP**, a API pode responder **200** com a mesma mensagem de sucesso padrão quando o corpo foi aceito e encaminhado para o servidor **ou** quando o payload foi **enfileirado** para retry — a distinção é registrada em **log** (ex.: prefixo `[MOP retry]`). Não altere o contrato do integrador assumindo erro só porque houve falha transitória no MOP; consulte logs e métricas.

**Retentativa manual:** o integrador pode chamar de novo o mesmo endpoint com o mesmo `X-Correlation-Id` para auditoria; a política de backoff e limites de tentativa é responsabilidade do cliente.

---

## 2. Tempos (cache)

Os **TTLs de cache** definem com que frequência o gateway **atualiza** dados mantidos em memória (após expirar a entrada), não o “intervalo de reprocessamento” de uma mensagem HTTP.

| O que é cacheado | Propriedade | Variável típica | Padrão (s) |
|------------------|-------------|-----------------|------------|
| Especificação OpenAPI embarcada | `cache.open-api-spec.ttl-seconds` | `CACHE_OPEN_API_SPEC_TTL_SECONDS` | 3600 |
| Configuração de campos (GET externo) | `cache.app-config.ttl-seconds` | `CACHE_APP_CONFIG_TTL_SECONDS` | 1800 |
| Endpoints normalizados | `cache.normalized-endpoints.ttl-seconds` | `CACHE_NORMALIZED_ENDPOINTS_TTL_SECONDS` | 300 |

Exemplo:

```bash
export SPRING_PROFILES_ACTIVE=local
export CACHE_APP_CONFIG_TTL_SECONDS=600
mvn spring-boot:run
```

---

## 3. Intervalos de retry, DLQ e disponibilidade

Propriedades `mop.client.retry.replay.*`, `mop.client.retry.dlq.*` e `mop.server.availability.*` controlam o **replay** da fila, o encaminhamento para **Dead-Letter Queue (DLQ)** e as **sondas** ao MOP. Lista completa: [VARIAVEIS_DE_AMBIENTE.md](VARIAVEIS_DE_AMBIENTE.md).

### 3.1 Limite de tentativas e DLQ

Cada mensagem na fila de retry mantém um contador `attemptCount`. Quando o `ClientRetryReplayScheduler` tenta reenviar ao MOP e a operação falha:

1. O contador é incrementado e os metadados `lastFailureAt` / `lastFailureDetail` são preenchidos.
2. Se `attemptCount < mop.client.retry.dlq.max-attempts` (padrão **5**), a mensagem é **republicada** na fila de retry.
3. Se o limite for atingido, o evento vai para a fila DLQ (`mop.client.retry.dlq.queue`, padrão `mop.client.retry.dlq`) com `dlqReason=MAX_RETRY_ATTEMPTS_EXCEEDED` e `movedToDlqAt`.

Mensagens **inválidas** (JSON corrompido) são enviadas à DLQ com `dlqReason=UNPARSEABLE_MESSAGE` e `rawQueuePayload` para rastreabilidade. Logs usam o prefixo `[MOP retry DLQ]` e incluem `correlationId`.

### 3.2 Reprocessamento a partir da DLQ

O gateway **não** consome nem reenvia automaticamente mensagens da DLQ. Após análise e correção, o **cliente/operador** é responsável por:

- consumir a fila `mop.client.retry.dlq` (RabbitMQ Management, script ou integração própria);
- inspecionar o JSON (`correlationId`, `dlqReason`, `lastFailureDetail`, payloads);
- reenviar ao fluxo principal (nova chamada HTTP ao gateway ou republicação manual na fila de retry), conforme a política do integrador.

---

## 4. Propriedades `spring.rabbitmq` em `application-local.yml`

Além da fila de retry, o YAML ainda declara *listener*, nomes de filas antigas e `spring.rabbitmq.retry` — legado de configurações anteriores. O que importa para o fluxo atual é o **broker** e a fila **`mop.client.retry.queue`**.

---

## 5. Resumo

| Pergunta | Resposta |
|----------|----------|
| Existe fila interna? | Sim — **retry** (`MOP_CLIENT_RETRY_QUEUE`) quando o MOP não está acessível no momento. |
| O que acontece após muitas falhas de replay? | Mensagem vai para a **DLQ** (`MOP_CLIENT_RETRY_DLQ_QUEUE`) após `MOP_CLIENT_RETRY_DLQ_MAX_ATTEMPTS` tentativas. |
| Como reprocessar da DLQ? | Responsabilidade do **cliente** — consumir a DLQ, corrigir e reenviar (HTTP ou fila de retry). |
| O cliente HTTP sempre vê erro quando há fila? | Não — pode receber **200** com mensagem de sucesso; ver logs `[MOP retry]`. |
| O que ajusta “frequência de atualização” de regras/config? | Principalmente **TTL dos caches** (`CACHE_*_TTL_SECONDS`). |

Sandbox e URLs: [README](../README.md).
