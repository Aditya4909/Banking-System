# Multi-Stage Dockerfile for JavaBank Cloud Web Application

# Stage 1: Build & Package with Maven
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy project files and build executable jar
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy packaged jar from builder stage
COPY --from=builder /build/target/javabank-1.0-SNAPSHOT.jar app.jar

# Expose default HTTP port
EXPOSE 8080

# Environment variables
ENV PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC"

# Run Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
