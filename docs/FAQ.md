# FAQ - Perguntas Frequentes

Este documento responde às perguntas mais comuns e problemas frequentes ao usar o MOP Client.

## Erros Comuns

### 1. Erro de conexão com RabbitMQ

**Sintoma:**
```
Connection refused: connect
```

**Solução:**
1. Verificar se o RabbitMQ está rodando:
   ```bash
   docker ps | grep rabbitmq
   ```

2. Verificar se as variáveis de ambiente estão corretas:
   - `RABBITMQ_VALIDATOR_HOST`
   - `RABBITMQ_VALIDATOR_PORT`
   - `RABBITMQ_USERNAME`
   - `RABBITMQ_PASSWORD`

3. Iniciar o RabbitMQ se necessário:
   ```bash
   docker-compose up -d
   ```

### 2. Header 'applicationMode' inválido

**Sintoma:**
```json
{
  "status": "ERROR",
  "error": "Invalid header",
  "details": "Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'"
}
```

**Solução:**
- Verificar se o header `applicationMode` está presente na requisição
- Validar que o valor é exatamente `TRANSMITTER` ou `RECEIVER` (case-sensitive)

### 3. Mensagens não são consumidas

**Sintoma:**
- Mensagens ficam na fila e não são processadas
- Logs mostram que o listener não está ativo

**Solução:**
1. Verificar logs da aplicação para erros
2. Verificar se o RabbitMQ está acessível
3. Verificar configuração de filas
4. Reiniciar a aplicação se necessário

### 4. Variável de ambiente não encontrada

**Sintoma:**
```
Could not resolve placeholder 'RABBITMQ_VALIDATOR_HOST' in value "${RABBITMQ_VALIDATOR_HOST}"
```

**Solução:**
- Para profiles `dev` e `homolog`, todas as variáveis são obrigatórias
- Verificar se todas as variáveis estão exportadas/configuradas
- Consulte [VARIAVEIS_DE_AMBIENTE.md](VARIAVEIS_DE_AMBIENTE.md) para lista completa

### 5. Porta já em uso

**Sintoma:**
```
Port 8080 is already in use
```

**Solução:**
1. Identificar processo usando a porta:
   ```bash
   # Linux/macOS
   lsof -i :8080
   
   # Windows
   netstat -ano | findstr :8080
   ```

2. Parar o processo ou alterar a porta:
   ```bash
   export SERVER_PORT=8081
   ```

## Configuração

### Como alterar o profile?

**Opções:**
1. Variável de ambiente:
   ```bash
   export SPRING_PROFILES_ACTIVE=dev
   ```

2. JVM argument:
   ```bash
   java -jar app.jar --spring.profiles.active=dev
   ```

3. Maven:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Como configurar retry?

Configure as variáveis de ambiente:
- `RABBITMQ_RETRY_MAX_ATTEMPTS`: Número de tentativas (padrão: 5)
- `RABBITMQ_RETRY_BACKOFF`: Delay entre tentativas em ms (padrão: 2000)

Consulte [MENSAGERIA.md](MENSAGERIA.md) para mais detalhes.

### Como habilitar logs detalhados?

Configure as variáveis:
- `LOG_LEVEL_ROOT`: Nível de log raiz (padrão: INFO)
- `LOG_LEVEL_GATEWAY`: Nível de log do gateway (padrão: DEBUG)

Exemplo:
```bash
export LOG_LEVEL_ROOT=DEBUG
export LOG_LEVEL_GATEWAY=TRACE
```

## Performance

### Como melhorar o throughput?

1. Aumentar `RABBITMQ_CONCURRENCY`:
   ```bash
   export RABBITMQ_CONCURRENCY=5
   ```

2. Ajustar `RABBITMQ_PREFETCH`:
   ```bash
   export RABBITMQ_PREFETCH=20
   ```

3. Monitorar uso de recursos e ajustar conforme necessário

### Como monitorar a aplicação?

1. **Health Check**:
   ```bash
   curl http://localhost:8080/v1/anonymize/actuator/health
   ```

2. **Logs**:
   - Verificar logs da aplicação
   - Monitorar logs do RabbitMQ

3. **Management UI**:
   - Acessar http://localhost:15672
   - Verificar filas, conexões e mensagens

## Docker

### Como executar via Docker?

Consulte [EXECUCAO_DOCKER.md](EXECUCAO_DOCKER.md) para guia completo.

### Como verificar logs dos containers?

```bash
docker logs mop-gateway
docker logs mop-anonymization
docker logs mop-validator
docker logs rabbitmq
```

### Como parar todos os containers?

```bash
docker stop mop-gateway mop-anonymization mop-validator rabbitmq
# ou
docker-compose down
```

## Mensageria

### Como verificar mensagens nas filas?

1. **Via Management UI**:
   - Acessar http://localhost:15672
   - Navegar para "Queues"
   - Selecionar a fila desejada

2. **Via linha de comando**:
   ```bash
   rabbitmqadmin list queues
   ```

### O que fazer com mensagens na DLQ?

1. Analisar logs para identificar causa
2. Verificar formato das mensagens
3. Corrigir problema se possível
4. Reprocessar manualmente se necessário

Consulte [MENSAGERIA.md](MENSAGERIA.md) para mais informações sobre DLQ.

## Troubleshooting Geral

### Como verificar se a aplicação está funcionando?

1. Verificar health check:
   ```bash
   curl http://localhost:8080/v1/anonymize/actuator/health
   ```

2. Verificar logs de inicialização:
   - Buscar por mensagens de sucesso
   - Verificar se todos os componentes foram carregados

3. Testar endpoint:
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

### Como obter mais informações de debug?

1. Configurar logs em nível DEBUG ou TRACE
2. Verificar logs da aplicação
3. Verificar logs do RabbitMQ
4. Usar Management UI para monitoramento

## Suporte

Para mais informações:

- [Documentação Principal](../README.md)
- [Arquitetura](ARQUITETURA.md)
- [Execução Docker](EXECUCAO_DOCKER.md)
- [Variáveis de Ambiente](VARIAVEIS_DE_AMBIENTE.md)
- [Mensageria](MENSAGERIA.md)

