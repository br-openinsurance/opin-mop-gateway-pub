# Eclipse Temurin JRE 17 + Alpine 3.24 (runtime OS).
# Alpine NÃO inclui systemd/udev, eliminando por construção:
#   CVE-2026-40224 / CVE-2026-40225 / CVE-2026-40226 (libsystemd0, libudev1).
# Alpine 3.24 traz sqlite-libs 3.53.2+ e libgcrypt 1.11.3+ (CVE-2026-11822/11824, CVE-2026-41989).
FROM eclipse-temurin:17-jre-alpine-3.23 AS temurin

FROM alpine:3.24

# Dependências de runtime do Temurin (HTTPS, timezone, locale) — não vêm só com COPY do JRE.
RUN apk update && \
    apk upgrade --no-cache --available && \
    apk add --no-cache ca-certificates tzdata musl-locales musl-locales-lang && \
    rm -rf /var/cache/apk/*

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=temurin ${JAVA_HOME} ${JAVA_HOME}

WORKDIR /app

# O nome do arquivo é definido pelo finalName no pom.xml (gateway.jar).
COPY target/gateway.jar /app/mop-client-gateway.jar

EXPOSE 8081

CMD ["java", "-jar", "/app/mop-client-gateway.jar"]
