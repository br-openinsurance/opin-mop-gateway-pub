# Dependabot `com.rabbitmq:amqp-client` (#39, #40, #41)

| Campo | Valor |
|---|---|
| Pacote | `com.rabbitmq:amqp-client` |
| Patch único | **5.37.0** (≥ 5.36.1 fecha os três) |
| Status neste repositório (**1.0.8**) | **fixed** |

O Dependabot pede `5.36.1+`. A 5.36.1 não está no Maven Central; **5.37.0** cobre os três GHSA.

Uso neste gateway: **Spring AMQP** (`RabbitTemplate`) nas filas de retry/DLQ. **Não** há `RpcClient`/`RpcServer`, `JsonRpcServer`/`JsonRpcClient` nem `ConnectionFactoryConfigurator.load()`.

## #40 — UTF-8 malformado em shortstr (RPC)

Uma mensagem AMQP com UTF-8 inválido numa propriedade shortstr (ex.: `correlation-id`) pode desligar de forma permanente um **consumer RPC** Java: o decoder gera U+FFFD; no eco RPC o encoder estoura o limite de 255 bytes e lança `IllegalArgumentException`. A mensagem não é ack; o broker reentrega e derruba o próximo consumer.

- Afetados: `<= 5.35.0` · Patch: **5.36.0**
- Neste gateway: não é serviço RPC. Filas de retry usam `RabbitTemplate`.

## #39 — credenciais em texto na exception

Se `ConnectionFactoryConfigurator.load()` falha ao parsear a chave `uri`, a exception inclui a URI crua (usuário e senha).

- Afetados: `<= 5.34.0` · Patch: **5.35.0**
- Neste gateway: o broker vem de `spring.rabbitmq.host` / `port` / `username` / `password` (Spring Boot), não de `ConnectionFactoryConfigurator.load()`.

## #41 — `JSONReader` não termina (DoS)

`com.rabbitmq.tools.json.JSONReader.read()` entra em loop / OOM com JSON truncado em string ou comentário `//`. Alcance: `JsonRpcServer` / `JsonRpcClient` com o mapper padrão.

- Afetados: `<= 5.36.0` · Patch: **5.36.1**
- Neste gateway: JSON-RPC do client Rabbit **não é usado**.

## Versão no `pom.xml`

```xml
<rabbit-amqp-client.version>5.37.0</rabbit-amqp-client.version>
```
