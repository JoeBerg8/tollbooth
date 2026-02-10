# Build stage
FROM eclipse-temurin:23-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Download dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src
COPY config ./config

# Build application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Create non-root user and install su-exec for privilege drop
RUN addgroup -S appuser && adduser -S appuser -G appuser && \
    apk add --no-cache su-exec

# Copy built JAR
COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app

EXPOSE 8080

# Fix tokens dir ownership (volume mounts as root) then drop to appuser
ENTRYPOINT ["/bin/sh", "-c", "mkdir -p /app/tokens && chown -R appuser:appuser /app/tokens && exec su-exec appuser java -jar /app/app.jar"]
