# CVEs Alpine — imagem Docker (2026)

Achados **Critical** e **High** na camada OS da imagem `eclipse-temurin:17-jre-alpine` (policy **CS-ST009** maturity 4).

## Pacotes afetados

| Pacote | Versão vulnerável | CVEs | CVSS | Fix mínimo | Remediação |
|--------|-------------------|------|------|------------|------------|
| `sqlite-libs` | 3.51.2-r0 | CVE-2026-11822, CVE-2026-11824 | **9.8** | 3.53.2-r0 | Repositório Alpine **v3.24** (`apk upgrade`) |
| `libgcrypt` | 1.11.2-r0 | CVE-2026-41989 | **7.5** | 1.11.3-r0 | Repositório Alpine **v3.24** (`apk upgrade`) |

> O branch **v3.23** do Alpine ainda publica `sqlite-libs` 3.51.2-r0. A correção só aparece a partir do **v3.24** (`sqlite-libs` 3.53.2-r0, `libgcrypt` 1.12.2-r0).

## Remediação no Dockerfile

Build **multi-stage**: JRE Temurin copiado de `eclipse-temurin:17-jre-alpine-3.23` para runtime **`alpine:3.24`** (ainda não existe tag oficial `17-jre-alpine-3.24` no GHCR/Docker Hub):

```dockerfile
FROM eclipse-temurin:17-jre-alpine-3.23 AS temurin
FROM alpine:3.24
COPY --from=temurin /opt/java/openjdk /opt/java/openjdk
```

Quando `eclipse-temurin:17-jre-alpine-3.24` for publicada, simplificar para um único `FROM`.

## Verificação

Após rebuild, confirmar versões na imagem:

```bash
docker run --rm <imagem> sh -c "apk info sqlite-libs libgcrypt"
```

Esperado: `sqlite-libs-3.53.2-r0` (ou superior) e `libgcrypt` ≥ 1.11.3-r0.
