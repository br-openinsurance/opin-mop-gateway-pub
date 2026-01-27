# Usa uma imagem base do Eclipse Temurin (sucessor do OpenJDK)
# Base: Ubuntu 22.04 (Jammy) - Atualizada para corrigir vulnerabilidades de segurança
FROM eclipse-temurin:17-jre-jammy

# Atualiza os pacotes do sistema para corrigir vulnerabilidades conhecidas
# Especialmente importante para corrigir CVE-2025-68973 (GnuPG out-of-bounds write) 
# e outras 11 vulnerabilidades HIGH no ecossistema GnuPG
# O upgrade atualiza todos os pacotes instalados, incluindo gnupg, gnupg2, dirmngr, etc.
RUN apt-get update && \
    apt-get upgrade -y && \
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
