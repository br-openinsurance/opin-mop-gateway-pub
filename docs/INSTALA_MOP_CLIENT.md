# 📦 MOP Client --- Instalação via Helm Chart

Este documento descreve o processo de instalação do **MOP Client**
utilizando o Helm Chart publicado no **GitHub Container Registry
(GHCR)**.

------------------------------------------------------------------------

## 🔧 Pré-requisitos

Antes de iniciar, garanta que você possui:

-   Kubernetes 1.24+
-   Helm 3.8+ (com suporte a OCI)
-   Acesso ao registry `ghcr.io`
-   Token GitHub com permissão `read:packages`

Verifique a versão do Helm:

``` bash
helm version
```

------------------------------------------------------------------------

## 📄 1. Criar o arquivo `values-client.yaml`

Crie o arquivo:

``` bash
touch values-client.yaml
```

Exemplo mínimo funcional:

``` yaml
global:
  imageRegistry: ghcr.io/br-openinsurance

  imagePullSecrets:
    - name: ghcr-secret

```

⚠️ Importante: - Nunca versionar o `values-client.yaml`

------------------------------------------------------------------------

## 🚀 2. Instalar o MOP Client

``` bash
helm install mop-client   oci://ghcr.io/br-openinsurance/mop-client-chart/mop-client   --version 0.5.3   -f values-client.yaml
```

------------------------------------------------------------------------

## 🔄 3. Atualizar o MOP Client

``` bash
helm upgrade mop-client   oci://ghcr.io/br-openinsurance/mop-client-chart/mop-client   --version <NOVA_VERSAO>   -f values-client.yaml
```

------------------------------------------------------------------------

## 🔎 4. Verificação pós-instalação

``` bash
helm status mop-client
kubectl get pods -A
kubectl get ingress -A
kubectl get svc -A
```

------------------------------------------------------------------------

## 🗑️ 5. Desinstalação

``` bash
helm uninstall mop-client
```

------------------------------------------------------------------------

## 📞 6. Suporte

Para dúvidas técnicas ou ajustes de configuração, contate a equipe
responsável pelo MOP Client.
