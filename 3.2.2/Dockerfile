# ==============================================================================
# Multi-Stage Production Dockerfile for CD Pipeline
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package JAR
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace/app

# Copy Maven wrapper and POM first (leverages Docker layer cache)
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml

RUN chmod +x ./mvnw
# Download dependencies in isolated layer
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and package application
COPY src src
RUN ./mvnw clean package -DskipTests

# Extract layers using Spring Boot layertools for optimized container caching
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted/

# ------------------------------------------------------------------------------
# Stage 2: Minimal, Secure Runtime Image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install curl for health checking
RUN apk add --no-cache curl && \
    addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy extracted layers in order of least frequent change to most frequent change
COPY --from=builder /workspace/app/extracted/dependencies/ ./
COPY --from=builder /workspace/app/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/app/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/app/extracted/application/ ./

# Run container as non-root user for security
USER appuser:appgroup

# Expose standard application and actuator ports
EXPOSE 8080

# Configure container-aware JVM options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Docker native HEALTHCHECK leveraging Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
