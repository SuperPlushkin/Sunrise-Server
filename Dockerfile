# Multi-stage build
FROM gradle:8.7-jdk21 AS builder
WORKDIR /app

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || return 0

COPY src ./src
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jdk-jammy

# для Docker Hub
LABEL maintainer="superplushkin@mail.ru"
LABEL version="0.0.2"
LABEL description="Spring Boot Server (For Messenger <<Sunrise>>)"

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# Или другой настроенный порт
EXPOSE 10610

ENTRYPOINT ["java", "-jar", "app.jar"]