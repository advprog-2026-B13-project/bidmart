FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew ./gradlew
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV PORT=8080
ENV JAVA_OPTS=""

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ARG APP_VERSION=local
ENV APP_VERSION=${APP_VERSION}

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
