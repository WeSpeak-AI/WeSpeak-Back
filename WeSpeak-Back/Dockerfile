FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY lombok.config .
COPY api api
COPY core core
COPY modules modules

# MODULE_PATH: e.g. "modules/module-auth" or "api/api-gateway"
ARG MODULE_PATH=modules/module-auth
RUN chmod +x gradlew && \
    GRADLE_TASK=$(echo "${MODULE_PATH}" | sed 's|/|:|g') && \
    ./gradlew :${GRADLE_TASK}:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

ARG MODULE_PATH=modules/module-auth
COPY --from=builder /workspace/${MODULE_PATH}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
