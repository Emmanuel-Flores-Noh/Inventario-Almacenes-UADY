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
COPY sistema-cliente/src sistema-cliente/src

# Compilacion y empaquetado del modulo
RUN mvn -pl sistema-cliente -am clean package -DskipTests

# Stage 2: Runtime con dependencias X11 para GUI Swing / FlatLaf
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar artefacto empaquetado
COPY --from=builder /workspace/sistema-cliente/target/sistema-cliente.jar /app/sistema-cliente.jar

# Variables de entorno para conexion al Broker
ENV BROKER_HOST=broker
ENV BROKER_PORT=5000
ENV DISPLAY=:0

ENTRYPOINT ["java", "-jar", "/app/sistema-cliente.jar"]
