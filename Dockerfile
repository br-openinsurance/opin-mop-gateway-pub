# Eclipse Temurin (sucessor do OpenJDK) sobre Alpine.
# Alpine NÃO inclui systemd/udev, eliminando por construção:
#   CVE-2026-40224 / CVE-2026-40225 / CVE-2026-40226 (libsystemd0, libudev1).
# Mantém execução idêntica: `java -jar` sobre JRE 17.
FROM eclipse-temurin:17-jre-alpine

# Mantém pacotes do sistema atualizados (musl, ca-certificates, busybox, etc.).
# Remediação Alpine: CVE-2026-11822/11824 (sqlite-libs), CVE-2026-41989 (libgcrypt).
RUN apk update && \
    apk upgrade --no-cache sqlite-libs libgcrypt && \
    apk upgrade --no-cache && \
    rm -rf /var/cache/apk/*

WORKDIR /app

# O nome do arquivo é definido pelo finalName no pom.xml (gateway.jar).
COPY target/gateway.jar /app/mop-client-gateway.jar

EXPOSE 8081

CMD ["java", "-jar", "/app/mop-client-gateway.jar"]
