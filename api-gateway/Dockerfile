FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY discovery-service/build.gradle discovery-service/build.gradle
COPY api-gateway/build.gradle api-gateway/build.gradle
COPY auth-service/build.gradle auth-service/build.gradle
COPY leave-service/build.gradle leave-service/build.gradle
COPY notification-service/build.gradle notification-service/build.gradle
COPY api-gateway/src api-gateway/src
RUN gradle :api-gateway:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api-gateway/build/libs/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
