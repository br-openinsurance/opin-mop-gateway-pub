# Usa uma imagem base do Eclipse Temurin (sucessor do OpenJDK) 
FROM eclipse-temurin:17-jre-jammy

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia o arquivo JAR da aplicação para dentro do contêiner
# O nome do arquivo é definido pelo finalName no pom.xml (gateway.jar)
COPY target/gateway.jar /app/mop-client-gateway.jar

# Expõe a porta que a aplicação usa
EXPOSE 8081

# Comando para executar a aplicação
CMD ["java", "-jar", "/app/mop-client-gateway.jar"]
