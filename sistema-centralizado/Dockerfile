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
COPY sistema-centralizado/src sistema-centralizado/src

# Compilacion y empaquetado del modulo
RUN mvn -pl sistema-centralizado -am clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear directorio para almacenamiento de codigos QR generados
RUN mkdir -p /app/storage/qr

# Copiar artefacto empaquetado
COPY --from=builder /workspace/sistema-centralizado/target/sistema-centralizado.jar /app/sistema-centralizado.jar

# Variables de entorno por defecto
ENV DB_HOST=mariadb
ENV DB_PORT=3306
ENV DB_NAME=almacenes_uady
ENV DB_USER=uady_user
ENV DB_PASSWORD=uady_pass
ENV BROKER_HOST=broker
ENV BROKER_PORT=5000
ENV SERVICE_PORT=6000

EXPOSE 6000

ENTRYPOINT ["java", "-jar", "/app/sistema-centralizado.jar"]
