# ==============================================================================
# DonorNexa — Production Multi-Stage Dockerfile for Render Cloud Deployment
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build JAR with Maven & Eclipse Temurin JDK 21
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM first to leverage Docker layer caching
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Make wrapper script executable
RUN chmod +x ./mvnw

# Pre-fetch project dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy full application source code
COPY src ./src

# Build production executable JAR (skipping unit tests for fast cloud builds)
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Runtime Image with Eclipse Temurin JRE 21
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create unprivileged user for security compliance
RUN addgroup -S donornexa && adduser -S donornexa -G donornexa

# Create persistent storage directories for uploads
RUN mkdir -p uploads/photos uploads/licences logs && \
    chown -R donornexa:donornexa /app

# Copy packaged JAR from builder stage
COPY --from=builder --chown=donornexa:donornexa /build/target/*.jar app.jar

# Switch to unprivileged runtime user
USER donornexa

# Render automatically assigns a dynamic port via $PORT (defaults to 8080)
ENV PORT=8080
EXPOSE ${PORT}

# JVM Container Optimization flags:
# - MaxRAMPercentage=75: Prevents OOM kills in memory-constrained cloud tiers
# - server.port=${PORT}: Dynamically binds to Render's allocated port
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
