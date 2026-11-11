# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copiar archivos POM para cachear dependencias
COPY pom.xml .
COPY common/pom.xml common/
COPY sistema-centralizado/pom.xml sistema-centralizado/
COPY sistema-cliente/pom.xml sistema-cliente/
COPY broker/pom.xml broker/

# Copiar codigo fuente del modulo comun y del modulo objetivo
COPY common/src common/src
COPY broker/src broker/src

# Compilacion y empaquetado del modulo
RUN mvn -pl broker -am clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar artefacto empaquetado
COPY --from=builder /workspace/broker/target/broker.jar /app/broker.jar

# Puerto de escucha del intermediario (ServerSocket TCP)
ENV BROKER_PORT=5000

EXPOSE 5000

ENTRYPOINT ["java", "-jar", "/app/broker.jar"]
