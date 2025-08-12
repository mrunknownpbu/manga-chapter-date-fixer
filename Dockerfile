# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Install gradle
RUN apt-get update && apt-get install -y gradle && apt-get clean

# Copy build files first (for better Docker layer caching)
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Copy source code
COPY src/ ./src/

# Build the application
RUN gradle build --no-daemon

# Final runtime image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built fat JAR
COPY --from=builder /app/build/libs/manga-chapter-date-fixer.jar ./app.jar

# Copy default configuration file
COPY chapterReleaseDateProviders.yaml ./

# Set default config path for container usage
ENV CONFIG_PATH=/app/chapterReleaseDateProviders.yaml

# Expose the web server port
EXPOSE 1996

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD []