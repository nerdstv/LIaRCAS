FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY .mvn .mvn
COPY common-models common-models
COPY log-ingestion-service log-ingestion-service
COPY log-processing-service log-processing-service
COPY root-cause-service root-cause-service

RUN mvn -pl log-processing-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/log-processing-service/target/log-processing-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
