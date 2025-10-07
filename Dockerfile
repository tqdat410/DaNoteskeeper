# ===================================
# MULTI-STAGE BUILD FOR DATA NOTES KEEPER
# ===================================

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime stage
FROM openjdk:21-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory and user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Expose port
EXPOSE 9090

# Run the application with optimized JVM settings
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
