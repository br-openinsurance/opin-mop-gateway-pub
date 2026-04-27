# Usa uma imagem base do Eclipse Temurin (sucessor do OpenJDK)
# Base: Ubuntu 24.04 (Noble) - systemd 255+ elimina CVEs presentes em Jammy (systemd 249).
FROM eclipse-temurin:17-jre-noble

# Atualiza pacotes do sistema e remove libs systemd/udev (não usadas pela JRE).
# A purga é best-effort: se algum core dep travar, segue o build — Noble já não traz
# as versões vulneráveis.
RUN apt-get update && \
    apt-get upgrade -y && \
    (DEBIAN_FRONTEND=noninteractive apt-get -y purge --auto-remove libsystemd0 libudev1 2>/dev/null || true) && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia o arquivo JAR da aplicação para dentro do contêiner
# O nome do arquivo é definido pelo finalName no pom.xml (gateway.jar)
COPY target/gateway.jar /app/mop-client-gateway.jar

# Expõe a porta que a aplicação usa
EXPOSE 8081

# Comando para executar a aplicação
CMD ["java", "-jar", "/app/mop-client-gateway.jar"]
