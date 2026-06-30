# Instalação do MOP Client (Kubernetes / Helm)

Para implantar o **MOP Client** em ambiente **Kubernetes**, a forma recomendada é o **Helm Chart** publicado no **GitHub Container Registry (GHCR)**. O chart provisiona os recursos necessários (Deployment, Service, Ingress conforme `values`, secrets de pull, etc.) sem exigir montagem manual do manifesto da aplicação.

## Guia oficial

O passo a passo completo — pré-requisitos (Kubernetes 1.24+, Helm 3.8+ com OCI), criação do `values-client.yaml`, `helm install`, `helm upgrade`, verificação pós-instalação e desinstalação — está no repositório de publicação:

**[Instalação via Helm Chart — `INSTALA_MOP_CLIENT.md`](https://github.com/br-openinsurance/opin-mop-gateway-pub/blob/feat/mop-client-install/docs/INSTALA_MOP_CLIENT.md)**

> Utilize o branch **`feat/mop-client-install`** (ou o branch/tag indicado pela equipe MOP) até que o guia seja incorporado à linha principal do repositório.

## Resumo

| Item | Detalhe |
|------|---------|
| Chart | `oci://ghcr.io/br-openinsurance/mop-client-chart/mop-client` |
| Registry | `ghcr.io` (credencial `read:packages` no GitHub) |
| Configuração | Arquivo local `values-client.yaml` (não versionar segredos) |
| Exemplo de instalação | `helm install mop-client oci://ghcr.io/br-openinsurance/mop-client-chart/mop-client --version <versão> -f values-client.yaml` |

Após o deploy, configure variáveis de ambiente e endpoints MOP conforme [`VARIAVEIS_DE_AMBIENTE.md`](VARIAVEIS_DE_AMBIENTE.md) (URLs de **sandbox** e **produção**), **provisione as filas RabbitMQ** `mop.client.retry.queue` e `mop.client.retry.dlq` (duráveis — ver [Criação obrigatória das filas](VARIAVEIS_DE_AMBIENTE.md#criação-obrigatória-das-filas)) e valide o health em `{context-path}/actuator/health` (padrão `/v1/anonymize/actuator/health`).

| Ambiente | `EXTERNAL_REQUEST_URL` |
|----------|-------------------------|
| Sandbox | `https://mop-server-entrypoint-sandbox.opinbrasil.com.br/process` |
| Produção | `https://mop-server-entrypoint.opinbrasil.com.br/process` |

`EXTERNAL_API_DATA_ANONYMIZATION`: mesmo host, path `/anonymization-fields?schema=Consent`.

## Outros modos de implantação

- **Desenvolvimento local (Docker):** imagem GHCR + RabbitMQ — ver [README.md](../README.md#início-rápido--rodando-em-até-10-minutos).

```bash
docker pull ghcr.io/br-openinsurance/opin-mop-gateway-pub/open-insurance-mop-gateway:develop
```

- **Desenvolvimento a partir do código:** `mvn spring-boot:run` com Docker Compose (RabbitMQ) — requer clone do repositório para build local.
- Variáveis e context-path: [`VARIAVEIS_DE_AMBIENTE.md`](VARIAVEIS_DE_AMBIENTE.md).

Para dúvidas sobre versão do chart, valores Helm ou suporte à instalação, contate a equipe responsável pelo MOP Client (referência no guia oficial acima).
